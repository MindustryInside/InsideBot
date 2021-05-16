package inside.event;

import com.github.benmanes.caffeine.cache.*;
import discord4j.common.util.Snowflake;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.message.*;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.rest.util.Color;
import inside.data.entity.StarboardConfig;
import inside.data.service.EntityRetriever;
import inside.service.MessageService;
import inside.util.*;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.*;
import reactor.util.context.Context;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.Arrays;

import static inside.util.ContextUtil.*;
import static reactor.function.TupleUtils.function;

@Component
public class StarboardEventHandler extends ReactiveEventAdapter{
    private static final Color offsetColor = Color.of(0xffefc0), targetColor = Color.of(0xffd37f);
    private static final float lerpStep = 0.000001f;

    private final ReactionEmoji[] stars = {
            ReactionEmoji.unicode("\u2B50"),
            ReactionEmoji.unicode("\uD83C\uDF1F"),
            ReactionEmoji.unicode("\uD83D\uDCAB")
    };

    private final Cache<Snowflake, Snowflake> staredMessages = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofDays(7))
            .build();

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

        Mono<Long> emojisCount = event.getMessage().flatMapMany(message -> Flux.fromIterable(message.getReactions()))
                .filter(reaction -> Arrays.asList(stars).contains(reaction.getEmoji()))
                .reduce(0L, (i, reaction) -> i + reaction.getCount());

        return initContext.zipWith(starboardConfig).flatMap(function((context, config) -> emojisCount.flatMap(l -> {
            Snowflake targetId = staredMessages.getIfPresent(event.getMessageId());
            Snowflake channelId = config.starboardChannelId().orElse(null);
            if(!config.isEnable() || channelId == null){
                return Mono.empty();
            }

            if(l > config.lowerStarBarrier() && targetId == null){
                return event.getGuild().flatMap(guild -> guild.getChannelById(channelId))
                        .cast(GuildMessageChannel.class)
                        .zipWith(event.getMessage())
                        .zipWith(author, (tuple, user) -> Tuples.of(tuple.getT1(), tuple.getT2(), user))
                        .flatMap(function((channel, message, user) -> channel.createMessage(spec -> spec.setContent(messageService.format(
                                context, "starboard.format", stars[0].asUnicodeEmoji().map(ReactionEmoji.Unicode::getRaw)
                                        .orElseThrow(AssertionError::new),
                                l, DiscordUtil.getChannelMention(message.getChannelId())))
                                .setEmbed(embed -> embed.setFooter(DateTimeFormat.longDateTime()
                                        .withLocale(context.get(KEY_LOCALE))
                                        .withZone(context.get(KEY_TIMEZONE))
                                        .print(DateTime.now()), null)
                                        .setAuthor(user.getUsername(), null, user.getAvatarUrl())
                                        .setDescription(message.getContent())
                                        .setColor(lerp(offsetColor, targetColor, Mathf.round(l, lerpStep)))
                                        .addField(messageService.get(context, "starboard.source"), messageService.format(context, "starboard.jump",
                                                guildId.asString(), message.getChannelId().asString(), message.getId().asString()), false)
                                        .setImage(message.getAttachments().stream().map(Attachment::getUrl).findFirst().orElse(""))))
                                .doOnNext(target -> staredMessages.put(message.getId(), target.getId()))));
            }else if(targetId != null){
                return event.getGuild().flatMap(guild -> guild.getChannelById(channelId))
                        .cast(GuildMessageChannel.class)
                        .flatMap(channel -> channel.getMessageById(targetId))
                        .zipWith(event.getMessage())
                        .flatMap(function((target, source) -> target.edit(spec -> spec.setEmbed(embed -> embed.from(target.getEmbeds().get(0).getData())
                                .setColor(lerp(offsetColor, targetColor, Mathf.round(l, lerpStep))))
                                .setContent(messageService.format(context, "starboard.format",
                                        stars[(int)Mathf.clamp(l - 1, 0, stars.length - 1)]
                                                .asUnicodeEmoji().map(ReactionEmoji.Unicode::getRaw)
                                                .orElseThrow(AssertionError::new),
                                        l, DiscordUtil.getChannelMention(source.getChannelId()))))
                                .doOnNext(ignored -> staredMessages.put(source.getId(), targetId)))); // update cache
            }
            return Mono.empty();
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

        Mono<Long> emojisCount = event.getMessage().flatMapMany(message -> Flux.fromIterable(message.getReactions()))
                .filter(reaction -> Arrays.asList(stars).contains(reaction.getEmoji()))
                .reduce(0L, (i, reaction) -> i + reaction.getCount());

        Mono<Message> targetMessage = starboardConfig.flatMap(config -> Mono.justOrEmpty(staredMessages.getIfPresent(event.getMessageId()))
                .filter(ignored -> config.isEnable())
                .flatMap(targetId -> event.getGuild().flatMap(guild -> Mono.justOrEmpty(config.starboardChannelId())
                        .flatMap(guild::getChannelById))
                        .cast(GuildMessageChannel.class)
                        .flatMap(channel -> channel.getMessageById(targetId))));

        return initContext.flatMap(context -> emojisCount.flatMap(l -> targetMessage.filter(ignored -> l < 3)
                .flatMap(Message::delete)
                .switchIfEmpty(targetMessage.zipWith(event.getMessage())
                        .flatMap(function((target, source) -> target.edit(spec -> spec.setEmbed(embed -> embed.from(target.getEmbeds().get(0).getData())
                                .setColor(lerp(offsetColor, targetColor, Mathf.round(l, lerpStep))))
                                .setContent(messageService.format(context, "starboard.format",
                                        stars[(int)Mathf.clamp(l - 1, 0, stars.length - 1)]
                                                .asUnicodeEmoji().map(ReactionEmoji.Unicode::getRaw)
                                                .orElseThrow(AssertionError::new),
                                        l, DiscordUtil.getChannelMention(source.getChannelId()))))
                                .doOnNext(ignored -> staredMessages.put(source.getId(), target.getId())))) // update cache
                        .then(Mono.empty())))
                .contextWrite(context));
    }

    @Override
    public Publisher<?> onReactionRemoveAll(ReactionRemoveAllEvent event){
        Mono<StarboardConfig> starboardConfig = Mono.justOrEmpty(event.getGuildId())
                .flatMap(guildId -> entityRetriever.getStarboardConfigById(guildId));

        return starboardConfig.flatMap(config -> Mono.justOrEmpty(staredMessages.getIfPresent(event.getMessageId()))
                .filter(ignored -> config.isEnable())
                .flatMap(targetId -> event.getGuild().flatMap(guild -> Mono.justOrEmpty(config.starboardChannelId())
                        .flatMap(guild::getChannelById))
                        .cast(GuildMessageChannel.class)
                        .flatMap(channel -> channel.getMessageById(targetId)))
                .flatMap(Message::delete)
                .doFirst(() -> staredMessages.invalidate(event.getMessageId())));
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
        if(r < 0){
            r = 0;
        }else if(r > 1){
            r = 1;
        }

        if(g < 0){
            g = 0;
        }else if(g > 1){
            g = 1;
        }

        if(b < 0){
            b = 0;
        }else if(b > 1){
            b = 1;
        }

        return Color.of(r, g, b);
    }
}
