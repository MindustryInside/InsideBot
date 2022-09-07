package inside.event;

import discord4j.common.util.Snowflake;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.message.MessageDeleteEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveAllEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import discord4j.core.object.Embed;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import discord4j.rest.util.Color;
import inside.data.EntityRetriever;
import inside.data.entity.EmojiDataWithPeriod;
import inside.data.entity.ImmutableStarboard;
import inside.data.entity.ImmutableStarboardConfig;
import inside.data.entity.StarboardConfig;
import inside.data.entity.base.ConfigEntity;
import inside.service.MessageService;
import inside.util.ContextUtil;
import inside.util.Mathf;
import inside.util.MessageUtil;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.math.MathFlux;
import reactor.util.context.Context;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static discord4j.core.object.entity.Message.Type.*;
import static reactor.function.TupleUtils.function;
import static reactor.function.TupleUtils.predicate;

public class StarboardEventHandler extends ReactiveEventAdapter {
    private static final EnumSet<Message.Type> representableTypes = EnumSet.of(GUILD_MEMBER_JOIN,
            USER_PREMIUM_GUILD_SUBSCRIPTION_TIER_1, USER_PREMIUM_GUILD_SUBSCRIPTION_TIER_2,
            USER_PREMIUM_GUILD_SUBSCRIPTION_TIER_3);
    private static final Color offsetColor = Color.of(0xffefc0), targetColor = Color.of(0xdaa520);
    private static final float lerpStep = 1.0E-05f;

    private final EntityRetriever entityRetriever;
    private final MessageService messageService;

    public StarboardEventHandler(EntityRetriever entityRetriever, MessageService messageService) {
        this.entityRetriever = Objects.requireNonNull(entityRetriever, "entityRetriever");
        this.messageService = Objects.requireNonNull(messageService, "messageService");
    }

    @Override
    public Publisher<?> onReactionAdd(ReactionAddEvent event) {
        Snowflake guildId = event.getGuildId().orElse(null);
        if (guildId == null) {
            return Mono.empty();
        }

        Mono<Context> initContext = entityRetriever.getGuildConfigById(guildId)
                .switchIfEmpty(entityRetriever.createGuildConfig(guildId))
                .map(config -> Context.of(ContextUtil.KEY_LOCALE, config.locale(),
                        ContextUtil.KEY_TIMEZONE, config.timezone()));

        Mono<ImmutableStarboardConfig> starboardConfig = entityRetriever.getStarboardConfigById(guildId)
                .filter(ConfigEntity::enabled);

        return Mono.zip(initContext, starboardConfig)
                .flatMap(function((context, config) -> {
                    List<ReactionEmoji> emojis = config.emojis().stream()
                            .map(EmojiDataWithPeriod::emoji)
                            .map(ReactionEmoji::of)
                            .toList();

                    if (!emojis.contains(event.getEmoji())) {
                        return Mono.empty();
                    }

                    Mono<Long> emojiCount = event.getMessage()
                            .flatMapMany(message -> Flux.fromIterable(message.getReactions())
                                    .filter(reaction -> emojis.contains(reaction.getEmoji()))
                                    .flatMap(r -> config.selfStarring() ? Mono.just((long) r.getCount()) :
                                            message.getReactors(r.getEmoji())
                                                    .filter(u -> !message.getAuthor().map(User::getId)
                                                            .map(id -> u.getId().equals(id))
                                                            .orElse(false))
                                                    .count()))
                            .as(MathFlux::max)
                            .defaultIfEmpty(0L)
                            .filter(l -> l >= config.threshold());

                    Snowflake channelId = Snowflake.of(config.starboardChannelId());
                    Mono<Message> sourceMessage = event.getMessage();

                    Mono<GuildMessageChannel> starboardChannel = event.getClient().getChannelById(channelId)
                            .cast(GuildMessageChannel.class);

                    return Mono.zip(emojiCount, sourceMessage)
                            .filter(predicate((count, source) -> source.getInteraction().isEmpty() &&
                                    source.getWebhookId().isEmpty() && !isStarboard(source)))
                            .filter(predicate((count, source) -> config.selfStarring() || source.getAuthor()
                                    .map(u -> !u.getId().equals(event.getUserId()))
                                    .orElse(true)))
                            .flatMap(function((count, source) -> {
                                AtomicReference<ImmutableStarboard> starboard = new AtomicReference<>();

                                Mono<Void> createNew = starboardChannel.flatMap(channel -> {
                                    var embedSpec = EmbedCreateSpec.builder();
                                    computeEmbed(context, source, guildId, embedSpec);
                                    embedSpec.color(lerp(offsetColor, targetColor, Mathf.round(count / 6f, lerpStep)));

                                    return channel.createMessage(MessageCreateSpec.builder()
                                                    .content("%s **%s** %s".formatted(
                                                            MessageUtil.getEmojiString(getCurrentEmoji(config, count).emoji()), count,
                                                            MessageUtil.getChannelMention(source.getChannelId())))
                                                    .addEmbed(embedSpec.build())
                                                    .build())
                                            .flatMap(target -> {
                                                var st = starboard.get();
                                                if (st != null) {
                                                    return entityRetriever.save(st.withTargetMessageId(target.getId().asLong()));
                                                }
                                                return entityRetriever.createStarboard(guildId, source.getId(), target.getId());
                                            })
                                            .then();
                                });

                                return entityRetriever.getStarboardById(guildId, event.getMessageId())
                                        .switchIfEmpty(createNew.then(Mono.empty()))
                                        .flatMap(board -> event.getClient().getMessageById(channelId, Snowflake.of(board.targetMessageId()))
                                                // Старборд удалён, но запись для него есть, значит просто создаём ембед, а запись нет
                                                .switchIfEmpty(Mono.defer(() -> {
                                                    starboard.set(board);
                                                    return createNew.then(Mono.empty());
                                                })))
                                        // обновляем старый
                                        .flatMap(target -> {

                                            var embeds = target.getEmbeds();
                                            Embed old = !embeds.isEmpty() ? embeds.get(0) : null;
                                            var embedSpec = EmbedCreateSpec.builder();

                                            if (old == null) { // someone remove embed
                                                computeEmbed(context, source, guildId, embedSpec);
                                            } else {
                                                updateEmbed(context, old, embedSpec);
                                            }

                                            embedSpec.color(lerp(offsetColor, targetColor, Mathf.round(count / 6f, lerpStep)));

                                            return target.edit(MessageEditSpec.builder()
                                                    .addEmbed(embedSpec.build())
                                                    .contentOrNull("%s **%s** %s".formatted(
                                                            MessageUtil.getEmojiString(getCurrentEmoji(config, count).emoji()),
                                                            count, MessageUtil.getChannelMention(source.getChannelId())))
                                                    .build());
                                        });
                            }))
                            .contextWrite(context);
                }));
    }

    @Override
    public Publisher<?> onReactionRemove(ReactionRemoveEvent event) {
        ReactionEmoji emoji = event.getEmoji();
        Snowflake guildId = event.getGuildId().orElse(null);
        if (guildId == null) {
            return Mono.empty();
        }

        Mono<Context> initContext = entityRetriever.getGuildConfigById(guildId)
                .switchIfEmpty(entityRetriever.createGuildConfig(guildId))
                .map(config -> Context.of(ContextUtil.KEY_LOCALE, config.locale(),
                        ContextUtil.KEY_TIMEZONE, config.timezone()));

        Mono<ImmutableStarboardConfig> starboardConfig = entityRetriever.getStarboardConfigById(guildId)
                .filter(ConfigEntity::enabled);

        return Mono.zip(initContext, starboardConfig)
                .flatMap(function((context, config) -> {
                    List<ReactionEmoji> emojis = config.emojis().stream()
                            .map(EmojiDataWithPeriod::emoji)
                            .map(ReactionEmoji::of)
                            .toList();

                    // prevents recursive starboard
                    if (!emojis.contains(emoji)) {
                        return Mono.empty();
                    }

                    Mono<Long> emojiCount = event.getMessage()
                            .flatMapMany(message -> Flux.fromIterable(message.getReactions())
                                    .filter(reaction -> emojis.contains(reaction.getEmoji()))
                                    .flatMap(r -> config.selfStarring() ? Mono.just((long) r.getCount()) :
                                            message.getReactors(r.getEmoji())
                                                    .filter(u -> !message.getAuthor().map(User::getId)
                                                            .map(id -> u.getId().equals(id))
                                                            .orElse(false))
                                                    .count()))
                            .as(MathFlux::max)
                            .defaultIfEmpty(0L);

                    Snowflake channelId = Snowflake.of(config.starboardChannelId());
                    Mono<Message> sourceMessage = event.getMessage();

                    Mono<Message> targetMessage = entityRetriever.getStarboardById(guildId, event.getMessageId())
                            .flatMap(board -> event.getClient().getMessageById(channelId, Snowflake.of(board.targetMessageId())));

                    return Mono.zip(emojiCount, targetMessage)
                            .flatMap(function((count, target) -> {
                                if (count < config.threshold()) {
                                    return target.delete().then(Mono.empty());
                                }

                                return Mono.zip(Mono.just(count), Mono.just(target), sourceMessage);
                            }))
                            // interaction filter
                            .filter(predicate((count, target, source) -> source.getInteraction().isEmpty() &&
                                    source.getWebhookId().isEmpty() && !isStarboard(source))) // prevents recursive starboard
                            // self-starring checks
                            .filter(predicate((count, target, source) -> config.selfStarring() || source.getAuthor()
                                    .map(u -> !u.getId().equals(event.getUserId()))
                                    .orElse(true)))
                            .flatMap(function((count, target, source) -> {
                                var embeds = target.getEmbeds();
                                Embed old = !embeds.isEmpty() ? embeds.get(0) : null;
                                var embedSpec = EmbedCreateSpec.builder();

                                if (old == null) { // someone remove embed
                                    computeEmbed(context, source, guildId, embedSpec);
                                } else {
                                    updateEmbed(context, old, embedSpec);
                                }

                                embedSpec.color(lerp(offsetColor, targetColor, Mathf.round(count / 6f, lerpStep)));

                                Snowflake sourceChannelId = event.getChannelId();

                                String text = "%s **%s** %s".formatted(
                                        MessageUtil.getEmojiString(getCurrentEmoji(config, count).emoji()),
                                        count, MessageUtil.getChannelMention(sourceChannelId));

                                return target.edit(MessageEditSpec.builder()
                                        .addEmbed(embedSpec.build())
                                        .contentOrNull(text)
                                        .build());
                            }))
                            .contextWrite(context);
                }));
    }

    @Override
    public Publisher<?> onReactionRemoveAll(ReactionRemoveAllEvent event) {
        Snowflake guildId = event.getGuildId().orElse(null);
        if (guildId == null) {
            return Mono.empty();
        }

        return entityRetriever.getStarboardConfigById(guildId)
                .filter(ConfigEntity::enabled)
                .flatMap(config -> entityRetriever.getStarboardById(guildId, event.getMessageId())
                        .flatMap(board -> event.getClient().getMessageById(Snowflake.of(board.targetMessageId()),
                                        Snowflake.of(board.targetMessageId()))
                                .flatMap(Message::delete)
                                .then(entityRetriever.delete(board))));
    }

    @Override
    public Publisher<?> onMessageDelete(MessageDeleteEvent event) {
        Snowflake guildId = event.getGuildId().orElse(null);
        if (guildId == null) {
            return Mono.empty();
        }

        // Если удалили target (т.е. саму досочку), то при последующем взаимодействии она будет пересоздана
        return entityRetriever.deleteStarboardBySourceId(guildId, event.getMessageId());
    }

    private void computeEmbed(Context ctx, Message source, Snowflake guildId, EmbedCreateSpec.Builder embedSpec) {
        var authorUser = source.getAuthor().orElseThrow();
        embedSpec.author(authorUser.getTag(), null, authorUser.getAvatarUrl());

        String content = MessageUtil.substringTo(source.getContent(), Embed.MAX_DESCRIPTION_LENGTH);
        if (content.isBlank() && representableTypes.contains(source.getType())) {
            content = messageService.getEnum(ctx, source.getType());
        }

        embedSpec.description(content);
        embedSpec.addField(messageService.get(null, "events.starboard.source-header"), String.format(messageService.get(null, "events.starboard.source-link"),
                guildId.asString(), source.getChannelId().asString(), source.getId().asString()), false);

        embedSpec.footer(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG)
                .withLocale(ctx.get(ContextUtil.KEY_LOCALE))
                .withZone(ctx.get(ContextUtil.KEY_TIMEZONE))
                .format(Instant.now()), null);

        var files = source.getAttachments().stream()
                .filter(att -> !att.getContentType()
                        .map(str -> str.startsWith("image"))
                        .orElse(false))
                .collect(Collectors.toSet());

        if (!files.isEmpty()) {
            embedSpec.addField((files.size() > 1 ? messageService.get(null, "events.starboard.files") : messageService.get(null, "events.starboard.file")), files.stream()
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

    private void updateEmbed(Context context, Embed old, EmbedCreateSpec.Builder embedSpec) {
        old.getDescription().ifPresent(embedSpec::description);
        var embedAuthor = old.getAuthor().orElseThrow();
        embedSpec.author(embedAuthor.getName().orElseThrow(), null,
                embedAuthor.getIconUrl().orElseThrow());
        embedSpec.fields(old.getFields().stream()
                .map(field -> EmbedCreateFields.Field.of(field.getName(), field.getValue(), field.isInline()))
                .collect(Collectors.toList()));
        embedSpec.footer(old.getFooter().map(footer -> EmbedCreateFields.Footer.of(
                footer.getText(), footer.getIconUrl().orElse(null))).orElseGet(() ->
                EmbedCreateFields.Footer.of(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG)
                        .withLocale(context.get(ContextUtil.KEY_LOCALE))
                        .withZone(context.get(ContextUtil.KEY_TIMEZONE))
                        .format(Instant.now()), null))); // backward fix

        old.getImage().map(Embed.Image::getUrl).ifPresent(embedSpec::image);
    }

    private boolean isStarboard(Message message) {
        var embeds = message.getEmbeds();
        if (embeds.size() != 1) {
            return false;
        }

        Embed embed = embeds.get(0);
        var fields = embed.getFields();
        return fields.size() >= 1 && embed.getFooter().isPresent() &&
                fields.stream().noneMatch(Embed.Field::isInline);
    }

    private static EmojiDataWithPeriod getCurrentEmoji(StarboardConfig config, long count) {
        var emojis = new ArrayList<>(config.emojis());
        emojis.sort(Comparator.comparingInt(EmojiDataWithPeriod::period));

        EmojiDataWithPeriod cnd = null;
        for (int i = 0, t = config.threshold(); i < emojis.size(); i++) {
            EmojiDataWithPeriod e = emojis.get(i);
            if (count >= t) {
                cnd = e;
            }

            t += e.period();
        }

        Objects.requireNonNull(cnd, "cnd");

        return cnd;
    }

    private static Color lerp(Color source, Color target, float t) {
        float r = source.getRed() / 255f;
        r += t * (target.getRed() / 255f - r);

        float g = source.getGreen() / 255f;
        g += t * (target.getGreen() / 255f - g);

        float b = source.getBlue() / 255f;
        b += t * (target.getBlue() / 255f - b);

        Color c = Color.of(Mathf.clamp(r), Mathf.clamp(g), Mathf.clamp(b));
        return toIntBits(c) < toIntBits(target) ? target : c;
    }

    private static int toIntBits(Color c) {
        return (int) (255f * c.getBlue()) << 16 | (int) (255f * c.getGreen()) << 8 | (int) (255f * c.getRed());
    }
}
