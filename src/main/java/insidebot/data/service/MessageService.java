package insidebot.data.service;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.*;
import insidebot.data.entity.MessageInfo;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Mono;

import java.util.Locale;

public interface MessageService{

    //bundle
    String get(String key);

    String format(String key, Object... args);

    //send
    Mono<Void> text(MessageChannel channel, String text, Object... args);

    Mono<Void> info(MessageChannel channel, String title, String text, Object... args);

    Mono<Void> err(MessageChannel channel, String text, Object... args);

    Mono<Void> err(MessageChannel channel, String title, String text, Object... args);

    //data
    MessageInfo getById( Snowflake messageId);

    boolean exists(Snowflake messageId);

    MessageInfo save(MessageInfo message);

    void delete(MessageInfo message);

    void deleteById(Snowflake memberId);
}
