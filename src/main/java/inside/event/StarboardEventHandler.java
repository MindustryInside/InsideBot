package inside.event;

import discord4j.common.util.Snowflake;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.message.*;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.reaction.*;
import discord4j.rest.util.*;
import inside.data.entity.*;
import inside.data.service.EntityRetriever;
import inside.service.MessageService;
import inside.util.*;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.*;
import reactor.math.MathFlux;
import reactor.util.context.Context;

import java.util.*;
import java.util.stream.Collectors;

import static inside.util.ContextUtil.*;
import static reactor.bool.BooleanUtils.*;
import static reactor.function.TupleUtils.function;

@Component
public class StarboardEventHandler extends ReactiveEventAdapter{
    private static final Color offsetColor = Color.of(0xffefc0), targetColor = Color.of(0xffd37f);
    private static final float lerpStep = 1.0E-15f;

    private final ReactionEmoji[] stars = {
            ReactionEmoji.unicode("\u2B50"),
            ReactionEmoji.unicode("\uD83C\uDF1F"),
            ReactionEmoji.unicode("\uD83D\uDCAB")
    };

    @Autowired
    private EntityRetriever entityRetriever;

    @Autowired
    private MessageService messageService;

    @Override
    public Publisher<?> onReactionAdd(ReactionAddEvent event){
        ReactionEmoji emoji = event.getEmoji();
        Snowflake guildId = event.getGuildId().orElse(null);
        Mono<User> author = event.getMessage().map(Message::getAuthor).flatMap(Mono::justOrEmpty);
        if(Arrays.stream(stars).noneMatch(emoji::equals) || guildId == null){
            return Mono.empty();
        }

        Mono<Context> initContext = entityRetriever.getGuildConfigById(guildId)
                .switchIfEmpty(entityRetriever.createGuildConfig(guildId))
                .map(guildConfig -> Context.of(KEY_LOCALE, guildConfig.locale(),
                        KEY_TIMEZONE, guildConfig.timeZone()));

        Mono<StarboardConfig> starboardConfig = entityRetriever.getStarboardConfigById(guildId);

        Mono<Integer> emojisCount = event.getMessage().flatMapMany(message -> Flux.fromIterable(message.getReactions()))
                .filter(reaction -> Arrays.asList(stars).contains(reaction.getEmoji()))
                .map(Reaction::getCount)
                .as(counts -> MathFlux.max(counts, Integer::compare));

        Mono<Starboard> starboard = entityRetriever.getStarboardById(guildId, event.getMessageId());

        return initContext.zipWith(starboardConfig).flatMap(function((context, config) -> emojisCount.flatMap(l -> {
            Snowflake channelId = config.starboardChannelId().orElse(null);
            if(!config.isEnable() || channelId == null){
                return Mono.empty();
            }

            Mono<GuildMessageChannel> starboardChannel = event.getGuild()
                    .flatMap(guild -> guild.getChannelById(channelId))
                    .cast(GuildMessageChannel.class);

            Mono<Message> targetMessage = starboard.zipWith(starboardChannel)
                    .flatMap(function((board, channel) -> channel.getMessageById(board.targetMessageId())));

            Mono<Void> updateOld = event.getMessage().zipWith(targetMessage)
                    .flatMap(function((source, target) -> target.edit(spec -> spec.setEmbed(embed -> embed.from(target.getEmbeds().get(0).getData())
                            .setColor(lerp(offsetColor, targetColor, Mathf.round(l, lerpStep))))
                            .setContent(messageService.format(context, "starboard.format",
                                    stars[Mathf.clamp(l - 1, 0, stars.length - 1)]
                                            .asUnicodeEmoji().map(ReactionEmoji.Unicode::getRaw)
                                            .orElseThrow(AssertionError::new),
                                    l, DiscordUtil.getChannelMention(source.getChannelId()))))))
                    .then();

            Mono<Void> createNew = Mono.zip(starboardChannel, event.getMessage(), author).flatMap(function((channel, source, user) ->
                    channel.createMessage(spec -> spec.setContent(messageService.format(
                            context, "starboard.format", stars[Mathf.clamp(l - 1, 0, stars.length - 1)]
                                    .asUnicodeEmoji().map(ReactionEmoji.Unicode::getRaw)
                                    .orElseThrow(AssertionError::new),
                            l, DiscordUtil.getChannelMention(source.getChannelId())))
                            .setAllowedMentions(AllowedMentions.suppressAll())
                            .setEmbed(embed -> {
                                embed.setFooter(DateTimeFormat.longDateTime()
                                        .withLocale(context.get(KEY_LOCALE))
                                        .withZone(context.get(KEY_TIMEZONE))
                                        .print(DateTime.now()), null);
                                embed.setAuthor(user.getUsername(), null, user.getAvatarUrl());
                                embed.setDescription(source.getContent());
                                embed.setColor(lerp(offsetColor, targetColor, Mathf.round(l, lerpStep)));
                                embed.addField(messageService.get(context, "starboard.source"), messageService.format(context, "starboard.jump",
                                        guildId.asString(), source.getChannelId().asString(), source.getId().asString()), false);
                                Set<Attachment> files = source.getAttachments().stream()
                                        .filter(att -> !att.getContentType().map(str -> str.startsWith("image")).orElse(false))
                                        .collect(Collectors.toSet());

                                if(files.size() != 0){
                                    String key = files.size() == 1 ? "starboard.attachment" : "starboard.attachments";
                                    embed.addField(messageService.get(context, key), files.stream()
                                            .map(att -> String.format("[%s](%s)", att.getFilename(), att.getUrl()))
                                            .collect(Collectors.joining("\n")), false);
                                }

                                source.getAttachments().stream()
                                        .map(Attachment::getUrl)
                                        .findFirst().ifPresent(embed::setImage);
                            }))
                            .flatMap(target -> entityRetriever.createStarboard(guildId, source.getId(), target.getId()))))
                    .then();

            return and(Mono.just(l > config.lowerStarBarrier()), not(starboard.hasElement()))
                    .flatMap(bool -> bool ? createNew : updateOld);
        }).contextWrite(context)));
    }

    @Override
    public Publisher<?> onReactionRemove(ReactionRemoveEvent event){
        ReactionEmoji emoji = event.getEmoji();
        Snowflake guildId = event.getGuildId().orElse(null);
        if(Arrays.stream(stars).noneMatch(emoji::equals) || guildId == null){
            return Mono.empty();
        }

        Mono<Context> initContext = entityRetriever.getGuildConfigById(guildId)
                .switchIfEmpty(entityRetriever.createGuildConfig(guildId))
                .map(guildConfig -> Context.of(KEY_LOCALE, guildConfig.locale(),
                        KEY_TIMEZONE, guildConfig.timeZone()));

        Mono<StarboardConfig> starboardConfig = entityRetriever.getStarboardConfigById(guildId);

        Mono<Integer> emojisCount = event.getMessage().flatMapMany(message -> Flux.fromIterable(message.getReactions()))
                .filter(reaction -> Arrays.asList(stars).contains(reaction.getEmoji()))
                .map(Reaction::getCount)
                .as(counts -> MathFlux.max(counts, Integer::compare))
                .defaultIfEmpty(0);

        Mono<Starboard> starboard = entityRetriever.getStarboardById(guildId, event.getMessageId());

        return initContext.zipWith(starboardConfig).flatMap(function((context, config) -> emojisCount.flatMap(i -> {
            Snowflake channelId = config.starboardChannelId().orElse(null);
            if(!config.isEnable() || channelId == null){
                return Mono.empty();
            }

            Mono<Message> targetMessage = event.getGuild().flatMap(guild -> guild.getChannelById(channelId))
                    .cast(GuildMessageChannel.class)
                    .flatMap(channel -> starboard.flatMap(board -> channel.getMessageById(board.targetMessageId())));

            return starboard.zipWhen(board -> event.getGuild().flatMap(guild -> guild.getChannelById(channelId))
                    .cast(GuildMessageChannel.class)
                    .flatMap(channel -> channel.getMessageById(board.targetMessageId())))
                    .flatMap(function((board, target) -> {
                        if(i <= config.lowerStarBarrier()){
                            return targetMessage.flatMap(Message::delete).then(entityRetriever.delete(board));
                        }

                        Snowflake sourceChannelId = event.getChannelId();
                        return target.edit(spec -> spec.setEmbed(embed -> embed.from(target.getEmbeds().get(0).getData())
                                .setColor(lerp(offsetColor, targetColor, Mathf.round(i, lerpStep))))
                                .setContent(messageService.format(context, "starboard.format",
                                        stars[Mathf.clamp(i - 1, 0, stars.length - 1)]
                                                .asUnicodeEmoji().map(ReactionEmoji.Unicode::getRaw)
                                                .orElseThrow(AssertionError::new),
                                        i, DiscordUtil.getChannelMention(sourceChannelId))));
                    }));
        }).contextWrite(context)));
    }

    @Override
    public Publisher<?> onReactionRemoveAll(ReactionRemoveAllEvent event){
        Snowflake guildId = event.getGuildId().orElse(null);
        if(guildId == null){
            return Mono.empty();
        }

        Mono<StarboardConfig> starboardConfig = entityRetriever.getStarboardConfigById(guildId);

        Mono<Starboard> starboard = entityRetriever.getStarboardById(guildId, event.getMessageId());

        return starboardConfig.flatMap(config -> {
            Snowflake channelId = config.starboardChannelId().orElse(null);
            if(!config.isEnable() || channelId == null){
                return Mono.empty();
            }

            return starboard.flatMap(board -> event.getGuild().flatMap(guild -> guild.getChannelById(channelId))
                    .cast(GuildMessageChannel.class)
                    .flatMap(channel -> channel.getMessageById(board.targetMessageId()))
                    .flatMap(Message::delete)
                    .then(entityRetriever.delete(board)));
        });
    }

    @Override
    public Publisher<?> onMessageDelete(MessageDeleteEvent event){
        Snowflake guildId = event.getGuildId().orElse(null);
        if(guildId == null){
            return Mono.empty();
        }

        return entityRetriever.deleteStarboardById(guildId, event.getMessageId());
    }

    public Color lerp(Color source, Color target, float t){
        float r = source.getRed()/255f;
        r += t * (target.getRed()/255f - r);

        float g = source.getGreen()/255f;
        g += t * (target.getGreen()/255f - g);

        float b = source.getBlue()/255f;
        b += t * (target.getBlue()/255f - b);

        return clamp(r, g, b);
    }

    private Color clamp(float r, float g, float b){
        r = Mathf.clamp(r, 0f, 1f);
        g = Mathf.clamp(g, 0f, 1f);
        b = Mathf.clamp(b, 0f, 1f);
        return Color.of(r, g, b);
    }
}
