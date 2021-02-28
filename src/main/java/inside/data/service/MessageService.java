package inside.data.service;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import inside.data.entity.MessageInfo;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import java.util.function.Consumer;

public interface MessageService{

    ReactionEmoji ok = ReactionEmoji.unicode("✅");

    //bundle

    String get(ContextView ctx, String key);

    String getCount(ContextView ctx, String key, long count);

    String getEnum(ContextView ctx, Enum<?> type);

    String format(ContextView ctx, String key, Object... args);

    //send

    Mono<Void> text(Mono<? extends MessageChannel> channel, String text);

    Mono<Void> info(Mono<? extends MessageChannel> channel, String title, String text);

    Mono<Void> info(Mono<? extends MessageChannel> channel, Consumer<EmbedCreateSpec> embed);

    Mono<Void> err(Mono<? extends MessageChannel> channel, String text);

    Mono<Void> err(Mono<? extends MessageChannel> channel, String title, String text);

    //data

    void awaitEdit(Snowflake messageId);

    void removeEdit(Snowflake messageId);

    boolean isAwaitEdit(Snowflake messageId);

    boolean isCleared(Snowflake messageId);

    void putMessage(Snowflake messageId);

    MessageInfo getById(Snowflake messageId);

    boolean exists(Snowflake messageId);

    void save(MessageInfo message);

    void deleteById(Snowflake messageId);

    void delete(MessageInfo message);

    void cleanUp();
}
