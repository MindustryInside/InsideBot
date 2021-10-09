package inside.event;

import discord4j.common.util.Snowflake;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.message.*;
import discord4j.core.object.Embed;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.*;
import discord4j.rest.util.Color;
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
import java.util.stream.Collectors;

import static inside.util.ContextUtil.*;
import static reactor.function.TupleUtils.*;

@Component
public class StarboardEventHandler extends ReactiveEventAdapter{
    private static final Duration fetchTimeout = Duration.ofSeconds(5);
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

        Mono<StarboardConfig> starboardConfig = entityRetriever.getStarboardConfigById(guildId);

        return Mono.zip(initContext, starboardConfig)
                .flatMap(function((context, config) -> {
                    Snowflake channelId = config.getStarboardChannelId().orElse(null);
                    List<ReactionEmoji> emojis = config.getEmojis().stream()
                            .map(ReactionEmoji::of)
                            .collect(Collectors.toList());

                    // prevents recursive starboard
                    if(!config.isEnabled() || channelId == null || !emojis.contains(emoji) || channelId.equals(event.getChannelId())){
                        return Mono.empty();
                    }

                    Mono<Long> emojiCount = event.getMessage()
                            .flatMapMany(message -> Flux.fromIterable(message.getReactions())
                                    .filter(reaction -> emojis.contains(reaction.getEmoji()))
                                    .flatMap(r -> config.isSelfStarring() ? Mono.just((long)r.getCount()) :
                                            message.getReactors(r.getEmoji())
                                                    .filter(u -> !u.getId().equals(event.getUserId()))
                                                    .count()))
                            .as(MathFlux::max)
                            .defaultIfEmpty(0L)
                            .filter(l -> l >= config.getLowerStarBarrier());

                    Mono<GuildMessageChannel> starboardChannel = event.getClient().getChannelById(channelId)
                            .cast(GuildMessageChannel.class);

                    Mono<Message> sourceMessage = event.getMessage();

                    return Mono.zip(emojiCount, starboardChannel, sourceMessage)
                            .filter(predicate((count, channel, source) ->
                                    source.getInteraction().isEmpty() && source.getWebhookId().isEmpty()))
                            .filter(predicate((count, channel, source) -> config.isSelfStarring()
                                    || source.getAuthor()
                                    .map(u -> !u.getId().equals(event.getUserId()))
                                    .orElse(true)))
                            .flatMap(function((count, channel, source) -> {

                                Mono<Message> findIfAbsent = channel.getLastMessageId()
                                        .map(channel::getMessagesBefore).orElse(Flux.empty())
                                        .filter(m -> isStarboard(m, source))
                                        .next()
                                        .flatMap(target -> entityRetriever.createStarboard(guildId, source.getId(), target.getId())
                                                .thenReturn(target))
                                        .timeout(fetchTimeout, Mono.empty());

                                Mono<Message> targetMessage = entityRetriever.getStarboardBySourceId(guildId, event.getMessageId())
                                        .flatMap(board -> channel.getMessageById(board.getTargetMessageId()))
                                        .switchIfEmpty(findIfAbsent);

                                List<String> formatted = emojis.stream()
                                        .map(DiscordUtil::getEmojiString)
                                        .collect(Collectors.toList());

                                Mono<Message> updateOld = targetMessage.flatMap(target -> {
                                    List<Embed> embeds = target.getEmbeds();
                                    Embed old = !embeds.isEmpty() ? embeds.get(0) : null;
                                    var embedSpec = EmbedCreateSpec.builder();

                                    if(old == null){ // someone remove embed
                                        computeEmbed(context, source, guildId, embedSpec);
                                    }else{
                                        updateEmbed(old, embedSpec);
                                    }

                                    embedSpec.color(lerp(offsetColor, targetColor, Mathf.round(count / 6f, lerpStep)));

                                    return target.edit(MessageEditSpec.builder()
                                            .addEmbed(embedSpec.build())
                                            .contentOrNull(messageService.format(context, "starboard.format",
                                                    formatted.get(Math.toIntExact(Mathf.clamp((count - 1) / 5, 0, formatted.size() - 1))),
                                                    count, DiscordUtil.getChannelMention(source.getChannelId())))
                                            .build());
                                });

                                Mono<Message> createNew = Mono.defer(() -> {
                                    var embedSpec = EmbedCreateSpec.builder();
                                    computeEmbed(context, source, guildId, embedSpec);
                                    embedSpec.color(lerp(offsetColor, targetColor, Mathf.round(count / 6f, lerpStep)));

                                    return channel.createMessage(MessageCreateSpec.builder()
                                                    .content(messageService.format(
                                                            context, "starboard.format", formatted.get(
                                                                    Math.toIntExact(Mathf.clamp(
                                                                    (count - 1) / 5, 0, formatted.size() - 1))),
                                                            count, DiscordUtil.getChannelMention(source.getChannelId())))
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

        return Mono.zip(initContext, starboardConfig)
                .flatMap(function((context, config) -> {
                    Snowflake channelId = config.getStarboardChannelId().orElse(null);
                    List<ReactionEmoji> emojis = config.getEmojis().stream()
                            .map(ReactionEmoji::of)
                            .collect(Collectors.toList());

                    // prevents recursive starboard
                    if(!config.isEnabled() || channelId == null || !emojis.contains(emoji) || channelId.equals(event.getChannelId())){
                        return Mono.empty();
                    }

                    Mono<Long> emojiCount = event.getMessage()
                            .flatMapMany(message -> Flux.fromIterable(message.getReactions())
                                    .filter(reaction -> emojis.contains(reaction.getEmoji()))
                                    .flatMap(r -> config.isSelfStarring() ? Mono.just((long)r.getCount()) :
                                            message.getReactors(r.getEmoji())
                                                    .filter(u -> !u.getId().equals(event.getUserId()))
                                                    .count()))
                            .as(MathFlux::max)
                            .defaultIfEmpty(0L)
                            .filter(l -> l >= config.getLowerStarBarrier());
                    Mono<GuildMessageChannel> starboardChannel = event.getClient().getChannelById(channelId)
                            .cast(GuildMessageChannel.class);

                    Mono<Message> sourceMessage = event.getMessage();

                    return Mono.zip(emojiCount, starboardChannel, sourceMessage)
                            .filter(predicate((count, channel, source) ->
                                    source.getInteraction().isEmpty() && source.getWebhookId().isEmpty()))
                            .filter(predicate((count, channel, source) -> config.isSelfStarring()
                                    || source.getAuthor()
                                    .map(u -> !u.getId().equals(event.getUserId()))
                                    .orElse(true)))
                            .flatMap(function((count, channel, source) -> {

                                Mono<Message> findIfAbsent = channel.getLastMessageId()
                                        .map(channel::getMessagesBefore).orElse(Flux.empty())
                                        .filter(m -> isStarboard(m, source))
                                        .next()
                                        .flatMap(target -> entityRetriever.createStarboard(guildId, source.getId(), target.getId())
                                                .thenReturn(target))
                                        .timeout(fetchTimeout, Mono.empty());

                                Mono<Message> targetMessage = entityRetriever.getStarboardBySourceId(guildId, event.getMessageId())
                                        .flatMap(board -> channel.getMessageById(board.getTargetMessageId()))
                                        .switchIfEmpty(findIfAbsent);

                                return targetMessage.flatMap(target -> {
                                    List<Embed> embeds = target.getEmbeds();
                                    Embed old = !embeds.isEmpty() ? embeds.get(0) : null;
                                    var embedSpec = EmbedCreateSpec.builder();

                                    if(old == null){ // someone remove embed
                                        computeEmbed(context, source, guildId, embedSpec);
                                    }else{
                                        updateEmbed(old, embedSpec);
                                    }

                                    embedSpec.color(lerp(offsetColor, targetColor, Mathf.round(count / 6f, lerpStep)));

                                    List<String> formatted = emojis.stream()
                                            .map(DiscordUtil::getEmojiString)
                                            .collect(Collectors.toList());

                                    Snowflake sourceChannelId = event.getChannelId();
                                    return target.edit(MessageEditSpec.builder()
                                            .addEmbed(embedSpec.build())
                                            .contentOrNull(messageService.format(context, "starboard.format",
                                                    formatted.get(Math.toIntExact(Mathf.clamp((count - 1) / 5, 0, formatted.size() - 1))),
                                                    count, DiscordUtil.getChannelMention(sourceChannelId)))
                                            .build());
                                });
                            }))
                            .contextWrite(context);
                }));
    }

    private void computeEmbed(Context context, Message source, Snowflake guildId, EmbedCreateSpec.Builder embedSpec){
        var authorUser = source.getAuthor().orElseThrow();
        embedSpec.author(authorUser.getTag(), null, authorUser.getAvatarUrl());

        String content = MessageUtil.substringTo(source.getContent(), Embed.MAX_DESCRIPTION_LENGTH);
        if(Strings.isEmpty(content) && messageService.hasEnum(context, source.getType())){
            content = messageService.getEnum(context, source.getType());
        }
        embedSpec.description(content);
        embedSpec.addField(messageService.get(context, "starboard.source"),
                messageService.format(context, "starboard.jump",
                        guildId.asString(), source.getChannelId().asString(),
                        source.getId().asString()), false);

        embedSpec.footer(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG)
                .withLocale(context.get(KEY_LOCALE))
                .withZone(context.get(KEY_TIMEZONE))
                .format(Instant.now()), null);

        Set<Attachment> files = source.getAttachments().stream()
                .filter(att -> !att.getContentType()
                        .map(str -> str.startsWith("image"))
                        .orElse(false))
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
                .filter(att -> att.getContentType()
                        .map(str -> str.startsWith("image"))
                        .orElse(false))
                .map(Attachment::getUrl)
                .findFirst().ifPresent(embedSpec::image);
    }

    private void updateEmbed(Embed old, EmbedCreateSpec.Builder embedSpec){
        old.getDescription().ifPresent(embedSpec::description);
        var embedAuthor = old.getAuthor().orElseThrow();
        embedSpec.author(embedAuthor.getName().orElseThrow(), null,
                embedAuthor.getIconUrl().orElseThrow());
        embedSpec.fields(old.getFields().stream()
                .map(field -> EmbedCreateFields.Field.of(field.getName(), field.getValue(), field.isInline()))
                .collect(Collectors.toList()));
        old.getImage().map(Embed.Image::getUrl).ifPresent(embedSpec::image);
    }

    @Override
    public Publisher<?> onReactionRemoveAll(ReactionRemoveAllEvent event){
        Snowflake guildId = event.getGuildId().orElse(null);
        if(guildId == null){
            return Mono.empty();
        }

        Mono<StarboardConfig> starboardConfig = entityRetriever.getStarboardConfigById(guildId);

        Mono<Starboard> starboard = entityRetriever.getStarboardBySourceId(guildId, event.getMessageId());

        return starboardConfig.flatMap(config -> {
            Snowflake channelId = config.getStarboardChannelId().orElse(null);
            if(!config.isEnabled() || channelId == null){
                return Mono.empty();
            }

            return starboard.flatMap(board -> event.getClient().getChannelById(channelId)
                    .cast(GuildMessageChannel.class)
                    .flatMap(channel -> channel.getMessageById(board.getTargetMessageId()))
                    .flatMap(Message::delete)
                    .then(entityRetriever.delete(board)));
        });
    }

    private boolean isStarboard(Message possibleTarget, Message source){
        List<Embed> embeds = possibleTarget.getEmbeds();
        if(embeds.size() != 1){
            return false;
        }

        List<Embed.Field> fields = embeds.get(0).getFields();
        String messageIdString = source.getId().asString();
        return fields.size() >= 1 && fields.get(0).getValue().equals(messageIdString + ")");
    }

    @Override
    public Publisher<?> onMessageDelete(MessageDeleteEvent event){
        return Mono.justOrEmpty(event.getGuildId()).flatMap(guildId ->
                entityRetriever.deleteStarboardById(guildId, event.getMessageId()));
    }

    private static Color lerp(Color source, Color target, float t){
        float r = source.getRed() / 255f;
        r += t * (target.getRed() / 255f - r);

        float g = source.getGreen() / 255f;
        g += t * (target.getGreen() / 255f - g);

        float b = source.getBlue() / 255f;
        b += t * (target.getBlue() / 255f - b);

        Color c = Color.of(Mathf.clamp(r), Mathf.clamp(g), Mathf.clamp(b));
        return toIntBits(c) < toIntBits(target) ? target : c;
    }

    private static int toIntBits(Color c){
        return (int)(255f * c.getBlue()) << 16 | (int)(255f * c.getGreen()) << 8 | (int)(255f * c.getRed());
    }
}
