package insidebot.data.service;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.*;
import insidebot.data.entity.MessageInfo;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Mono;

import java.util.Locale;

public interface MessageService{

    //bundle
    String get(@NonNull String key);

    String get(@NonNull String key, Locale locale);

    String format(@NonNull String key, Object... args);

    String format(@NonNull String key, Locale locale, Object... args);

    //send
    Mono<Void> text(MessageChannel channel, String text, Object... args);

    Mono<Void> info(MessageChannel channel, String title, String text, Object... args);

    Mono<Void> err(MessageChannel channel, String text, Object... args);

    Mono<Void> err(MessageChannel channel, String title, String text, Object... args);

    //data
    MessageInfo getById(@NonNull Snowflake messageId);

    boolean exists(@NonNull Snowflake messageId);

    MessageInfo save(@NonNull MessageInfo message);

    void delete(@NonNull MessageInfo message);

    void deleteById(@NonNull Snowflake memberId);
}
