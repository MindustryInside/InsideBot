package inside.service;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import java.util.function.Consumer;

public interface MessageService extends MessageHolderService{

    ReactionEmoji ok = ReactionEmoji.unicode("✅");

    // bundle

    String get(ContextView ctx, String key);

    String getCount(ContextView ctx, String key, long count);

    String getEnum(ContextView ctx, Enum<?> type);

    String format(ContextView ctx, String key, Object... args);

    // send

    Mono<Void> text(Mono<? extends MessageChannel> channel, String text, Object... args);

    Mono<Void> info(Mono<? extends MessageChannel> channel, String title, String text, Object... args);

    Mono<Void> info(Mono<? extends MessageChannel> channel, Consumer<EmbedCreateSpec> embed);

    Mono<Void> err(Mono<? extends MessageChannel> channel, String text, Object... args);

    // TODO: rename
    Mono<Void> error(Mono<? extends MessageChannel> channel, String title, String text, Object... args);

    // data

    String encrypt(String text, Snowflake messageId, Snowflake channelId);

    String decrypt(String text, Snowflake messageId, Snowflake channelId);
}
