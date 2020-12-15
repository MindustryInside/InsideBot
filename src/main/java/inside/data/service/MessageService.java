package inside.data.service;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.*;
import inside.data.entity.MessageInfo;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

public interface MessageService{

    //bundle
    String get(String key);

    String getCount(String key, long count);

    String getEnum(Enum<?> type);

    String format(String key, Object... args);

    //send
    Mono<Void> text(Mono<? extends MessageChannel> channel, String text, Object... args);

    Mono<Void> info(Mono<? extends MessageChannel> channel, String title, String text, Object... args);

    Mono<Void> info(Mono<? extends MessageChannel> channel, Consumer<EmbedCreateSpec> embed);

    Mono<Void> err(Mono<? extends MessageChannel> channel, String text, Object... args);

    Mono<Void> err(Mono<? extends MessageChannel> channel, String title, String text, Object... args);

    //data
    boolean isCleared(Snowflake messageId);

    void putMessage(Snowflake messageId);

    MessageInfo getById(Snowflake messageId);

    boolean exists(Snowflake messageId);

    MessageInfo save(MessageInfo message);

    void delete(MessageInfo message);

    void deleteById(Snowflake memberId);

    void cleanUp();
}
