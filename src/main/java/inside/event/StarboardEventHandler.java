package inside.event;

import discord4j.common.util.Snowflake;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.message.*;
import discord4j.core.object.Embed;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.reaction.*;
import discord4j.core.spec.*;
import discord4j.rest.util.*;
import inside.data.entity.*;
import inside.data.service.EntityRetriever;
import inside.service.MessageService;
import inside.util.*;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.*;
import reactor.math.MathFlux;
import reactor.util.context.Context;

import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static inside.util.ContextUtil.*;
import static reactor.function.TupleUtils.function;

@Component
public class StarboardEventHandler extends ReactiveEventAdapter{
    private static final Color offsetColor = Color.of(0xffefc0), targetColor = Color.of(0xdaa520);
    private static final float lerpStep = 1.0E-05f;

    @Autowired
    private EntityRetriever entityRetriever;

    @Autowired
    private MessageService messageService;

    @Override
    public Publisher<?> onReactionAdd(ReactionAddEvent event){
        ReactionEmoji emoji = event.getEmoji();
        Snowflake guildId = event.getGuildId().orElse(null);
        if(guildId == null){
            return Mono.empty();
        }

        Mono<Context> initContext = entityRetriever.getGuildConfigById(guildId)
                .switchIfEmpty(entityRetriever.createGuildConfig(guildId))
                .map(guildConfig -> Context.of(KEY_LOCALE, guildConfig.locale(),
                        KEY_TIMEZONE, guildConfig.timeZone()));

        Function<List<ReactionEmoji>, Mono<Integer>> emojisCount = emojis -> event.getMessage()
                .flatMapIterable(Message::getReactions)
                .filter(reaction -> emojis.contains(reaction.getEmoji()))
                .map(Reaction::getCount)
                .as(MathFlux::max);

        Mono<StarboardConfig> starboardConfig = entityRetriever.getStarboardConfigById(guildId);

        return Mono.zip(initContext, starboardConfig)
                .flatMap(function((context, config) -> {
                    Snowflake channelId = config.starboardChannelId().orElse(null);
                    List<ReactionEmoji> emojis = config.emojis().stream()
                            .map(ReactionEmoji::of)
                            .collect(Collectors.toList());

                    if(!config.isEnabled() || channelId == null || !emojis.contains(emoji)){
                        return Mono.empty();
                    }

                    Mono<Integer> emojiCount = emojisCount.apply(emojis).filter(l -> l >= config.lowerStarBarrier());

                    Mono<GuildMessageChannel> starboardChannel = event.getClient().getChannelById(channelId)
                            .cast(GuildMessageChannel.class);

                    Mono<Message> sourceMessage = event.getMessage();

                    Mono<User> author = event.getMessage().map(Message::getAuthor).flatMap(Mono::justOrEmpty);

                    return Mono.zip(emojiCount, starboardChannel, sourceMessage, author)
                            .flatMap(function((count, channel, source, user) -> {
                                if(source.getInteraction().isPresent() || source.getWebhookId().isPresent()){
                                    return Mono.empty(); // don't handle webhooks and interactions
                                }

                                Mono<Message> findIfAbsent = channel.getLastMessageId()
                                        .map(channel::getMessagesBefore).orElse(Flux.empty())
                                        .take(Duration.ofSeconds(4))
                                        .filter(m -> m.getEmbeds().size() == 1 && m.getEmbeds().get(0).getFields().size() >= 1)
                                        .filter(m -> m.getEmbeds().get(0).getFields().get(0)
                                                .getValue().endsWith(source.getId().asString() + ")")) // match md link
                                        .next()
                                        .flatMap(target -> entityRetriever.createStarboard(guildId, source.getId(), target.getId())
                                                .thenReturn(target))
                                        .timeout(Duration.ofSeconds(6), Mono.empty());

                                Mono<Message> targetMessage = entityRetriever.getStarboardById(guildId, event.getMessageId())
                                        .flatMap(board -> channel.getMessageById(board.targetMessageId()))
                                        .switchIfEmpty(findIfAbsent);

                                List<String> formatted = emojis.stream()
                                        .map(DiscordUtil::getEmojiString)
                                        .collect(Collectors.toList());

                                Mono<Message> updateOld = targetMessage.flatMap(target -> {
                                    var old = Try.ofCallable(() -> target.getEmbeds().get(0)).orElse(null); // IOOB
                                    var embedSpec = EmbedCreateSpec.builder();

                                    if(old == null){ // someone remove embed

                                        var authorUser = source.getAuthor().orElseThrow(IllegalStateException::new);
                                        embedSpec.author(authorUser.getUsername(), null, authorUser.getAvatarUrl());

                                        embedSpec.description(source.getContent());
                                        embedSpec.footer(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG)
                                                .withLocale(context.get(KEY_LOCALE))
                                                .withZone(context.get(KEY_TIMEZONE))
                                                .format(Instant.now()), null);

                                        Set<Attachment> files = source.getAttachments().stream()
                                                .filter(att -> !att.getContentType().map(str -> str.startsWith("image")).orElse(false))
                                                .collect(Collectors.toSet());

                                        if(!files.isEmpty()){
                                            String key = files.size() == 1
                                                    ? "starboard.attachment"
                                                    : "starboard.attachments";
                                            embedSpec.addField(messageService.get(context, key), files.stream()
                                                    .map(att -> String.format("[%s](%s)%n", att.getFilename(), att.getUrl()))
                                                    .collect(Collectors.joining()), false);
                                        }

                                        source.getAttachments().stream()
                                                .filter(att -> att.getContentType().map(str -> str.startsWith("image")).orElse(false))
                                                .map(Attachment::getUrl)
                                                .findFirst().ifPresent(embedSpec::image);
                                    }else{

                                        old.getDescription().ifPresent(embedSpec::description);
                                        var embedAuthor = old.getAuthor().orElseThrow(IllegalStateException::new);
                                        embedSpec.author(embedAuthor.getName().orElseThrow(IllegalStateException::new), null,
                                                embedAuthor.getIconUrl().orElseThrow(IllegalStateException::new));
                                        embedSpec.fields(old.getFields().stream()
                                                .map(field -> EmbedCreateFields.Field.of(field.getName(), field.getValue(), field.isInline()))
                                                .collect(Collectors.toList()));
                                        old.getImage().map(Embed.Image::getUrl).ifPresent(embedSpec::image);
                                    }

                                    embedSpec.color(lerp(offsetColor, targetColor, Mathf.round(count / 6f, lerpStep)));

                                    return target.edit(MessageEditSpec.builder()
                                            .addEmbed(embedSpec.build())
                                            .contentOrNull(messageService.format(context, "starboard.format",
                                                    formatted.get(Mathf.clamp((count - 1) / 5, 0, formatted.size() - 1)),
                                                    count, DiscordUtil.getChannelMention(source.getChannelId())))
                                            .build());
                                });

                                Mono<Message> createNew = Mono.defer(() -> {

                                    var embedSpec = EmbedCreateSpec.builder();

                                    embedSpec.footer(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG)
                                            .withLocale(context.get(KEY_LOCALE))
                                            .withZone(context.get(KEY_TIMEZONE))
                                            .format(Instant.now()), null);
                                    embedSpec.author(user.getUsername(), null, user.getAvatarUrl());
                                    embedSpec.description(source.getContent());
                                    embedSpec.color(lerp(offsetColor, targetColor, Mathf.round(count / 6f, lerpStep)));
                                    embedSpec.addField(messageService.get(context, "starboard.source"),
                                            messageService.format(context, "starboard.jump",
                                            guildId.asString(), source.getChannelId().asString(), source.getId().asString()), false);
                                    Set<Attachment> files = source.getAttachments().stream()
                                            .filter(att -> !att.getContentType().map(str -> str.startsWith("image")).orElse(false))
                                            .collect(Collectors.toSet());

                                    if(!files.isEmpty()){
                                        String key = files.size() == 1
                                                ? "starboard.attachment"
                                                : "starboard.attachments";
                                        embedSpec.addField(messageService.get(context, key), files.stream()
                                                .map(att -> String.format("[%s](%s)%n", att.getFilename(), att.getUrl()))
                                                .collect(Collectors.joining()), false);
                                    }

                                    source.getAttachments().stream()
                                            .filter(att -> att.getContentType().map(str -> str.startsWith("image")).orElse(false))
                                            .map(Attachment::getUrl)
                                            .findFirst().ifPresent(embedSpec::image);

                                    return channel.createMessage(MessageCreateSpec.builder()
                                                    .content(messageService.format(
                                                            context, "starboard.format", formatted.get(Mathf.clamp(
                                                                    (count - 1) / 5, 0, formatted.size() - 1)),
                                                            count, DiscordUtil.getChannelMention(source.getChannelId())))
                                                    .allowedMentions(AllowedMentions.suppressAll())
                                                    .addEmbed(embedSpec.build())
                                                    .build())
                                            .flatMap(target -> entityRetriever.createStarboard(guildId, source.getId(), target.getId())
                                                    .thenReturn(target));
                                });

                                return updateOld.switchIfEmpty(createNew);
                            }))
                            .contextWrite(context);
                }));
    }

    @Override
    public Publisher<?> onReactionRemove(ReactionRemoveEvent event){
        ReactionEmoji emoji = event.getEmoji();
        Snowflake guildId = event.getGuildId().orElse(null);
        if(guildId == null){
            return Mono.empty();
        }

        Mono<Context> initContext = entityRetriever.getGuildConfigById(guildId)
                .switchIfEmpty(entityRetriever.createGuildConfig(guildId))
                .map(guildConfig -> Context.of(KEY_LOCALE, guildConfig.locale(),
                        KEY_TIMEZONE, guildConfig.timeZone()));

        Mono<StarboardConfig> starboardConfig = entityRetriever.getStarboardConfigById(guildId);

        Function<List<ReactionEmoji>, Mono<Integer>> emojisCount = emojis -> event.getMessage()
                .flatMapMany(message -> Flux.fromIterable(message.getReactions()))
                .filter(reaction -> emojis.contains(reaction.getEmoji()))
                .map(Reaction::getCount)
                .as(MathFlux::max)
                .defaultIfEmpty(0);

        return Mono.zip(initContext, starboardConfig)
                .flatMap(function((context, config) -> {
                    Snowflake channelId = config.starboardChannelId().orElse(null);
                    List<ReactionEmoji> emojis = config.emojis().stream()
                            .map(ReactionEmoji::of)
                            .collect(Collectors.toList());

                    if(!config.isEnabled() || channelId == null || !emojis.contains(emoji)){
                        return Mono.empty();
                    }

                    Mono<Integer> emojiCount = emojisCount.apply(emojis);

                    Mono<GuildMessageChannel> starboardChannel = event.getClient().getChannelById(channelId)
                            .cast(GuildMessageChannel.class);

                    Mono<Message> sourceMessage = event.getMessage();

                    return Mono.zip(emojiCount, starboardChannel, sourceMessage)
                            .flatMap(function((count, channel, source) -> {
                                if(source.getInteraction().isPresent() || source.getWebhookId().isPresent()){
                                    return Mono.empty(); // don't handle webhooks and interactions
                                }

                                Mono<Message> findIfAbsent = channel.getLastMessageId()
                                        .map(channel::getMessagesBefore).orElse(Flux.empty())
                                        .take(Duration.ofSeconds(4))
                                        .filter(m -> m.getEmbeds().size() == 1 && m.getEmbeds().get(0).getFields().size() >= 1)
                                        .filter(m -> m.getEmbeds().get(0).getFields().get(0)
                                                .getValue().endsWith(source.getId().asString() + ")")) // match md link
                                        .next()
                                        .flatMap(target -> entityRetriever.createStarboard(guildId, source.getId(), target.getId())
                                                .thenReturn(target))
                                        .timeout(Duration.ofSeconds(6), Mono.empty());

                                Mono<Message> targetMessage = entityRetriever.getStarboardById(guildId, event.getMessageId())
                                        .flatMap(board -> channel.getMessageById(board.targetMessageId()))
                                        .switchIfEmpty(findIfAbsent);

                                return targetMessage.flatMap(target -> {
                                    if(count < config.lowerStarBarrier()){
                                        return target.delete().and(entityRetriever.deleteStarboardById(guildId, event.getMessageId()));
                                    }

                                    var old = Try.ofCallable(() -> target.getEmbeds().get(0)).orElse(null); // IOOB
                                    var embedSpec = EmbedCreateSpec.builder();

                                    if(old == null){ // someone remove embed

                                        //TODO: implement
                                        return Mono.empty();
                                    }else{

                                        old.getDescription().ifPresent(embedSpec::description);
                                        var embedAuthor = old.getAuthor().orElseThrow(IllegalStateException::new);
                                        embedSpec.author(embedAuthor.getName().orElseThrow(IllegalStateException::new), null,
                                                embedAuthor.getIconUrl().orElseThrow(IllegalStateException::new));
                                        embedSpec.fields(old.getFields().stream()
                                                .map(field -> EmbedCreateFields.Field.of(field.getName(), field.getValue(), field.isInline()))
                                                .collect(Collectors.toList()));
                                        old.getImage().map(Embed.Image::getUrl).ifPresent(embedSpec::image);
                                    }

                                    embedSpec.color(lerp(offsetColor, targetColor, Mathf.round(count / 6f, lerpStep)));

                                    List<String> formatted = emojis.stream()
                                            .map(DiscordUtil::getEmojiString)
                                            .collect(Collectors.toList());

                                    Snowflake sourceChannelId = event.getChannelId();
                                    return target.edit(MessageEditSpec.builder()
                                            .addEmbed(embedSpec.build())
                                            .contentOrNull(messageService.format(context, "starboard.format",
                                                    formatted.get(Mathf.clamp((count - 1) / 5, 0, formatted.size() - 1)),
                                                    count, DiscordUtil.getChannelMention(sourceChannelId)))
                                            .build());
                                });
                            }))
                            .contextWrite(context);
                }));
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
            if(!config.isEnabled() || channelId == null){
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
        return Mono.justOrEmpty(event.getGuildId()).flatMap(guildId ->
                entityRetriever.deleteStarboardById(guildId, event.getMessageId()));
    }

    private Color lerp(Color source, Color target, float t){
        float r = source.getRed() / 255f;
        r += t * (target.getRed() / 255f - r);

        float g = source.getGreen() / 255f;
        g += t * (target.getGreen() / 255f - g);

        float b = source.getBlue() / 255f;
        b += t * (target.getBlue() / 255f - b);

        Color c = Color.of(Mathf.clamp(r), Mathf.clamp(g), Mathf.clamp(b));
        return toIntBits(c) < toIntBits(target) ? target : c;
    }

    private int toIntBits(Color c){
        return (int)(255f * c.getBlue()) << 16 | (int)(255f * c.getGreen()) << 8 | (int)(255f * c.getRed());
    }
}
