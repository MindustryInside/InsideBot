package inside.service;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.*;
import inside.command.model.CommandEnvironment;
import inside.interaction.InteractionEnvironment;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import java.util.*;
import java.util.function.Consumer;

public interface MessageService{

    ReactionEmoji ok = ReactionEmoji.unicode("✅");

    ReactionEmoji failed = ReactionEmoji.unicode("❌");

    // bundle

    String get(ContextView ctx, String key);

    String get(ContextView ctx, String key, String defaultKey);

    String getPluralized(ContextView ctx, String key, long count);

    String getEnum(ContextView ctx, Enum<?> type);

    boolean hasEnum(ContextView ctx, Enum<?> type);

    String format(ContextView ctx, String key, Object... args);

    Optional<Locale> getLocale(String str);

    Map<String, Locale> getSupportedLocales();

    Locale getDefaultLocale();

    // send (command)

    MessageCreateMono text(CommandEnvironment environment, String text, Object... args);

    MessageCreateMono info(CommandEnvironment environment, String text, Object... args);

    MessageCreateMono infoTitled(CommandEnvironment environment, String title, String text, Object... args);

    Mono<Void> err(CommandEnvironment environment, String text, Object... args);

    Mono<Void> errTitled(CommandEnvironment environment, String title, String text, Object... args);

    // send (interactive)
    // huh, nice names

    InteractionApplicationCommandCallbackReplyMono text(InteractionEnvironment environment, String text, Object... args);

    InteractionApplicationCommandCallbackReplyMono infoTitled(InteractionEnvironment environment, String title, String text, Object... args);

    InteractionApplicationCommandCallbackReplyMono info(InteractionEnvironment environment, String text, Object... args);

    InteractionApplicationCommandCallbackReplyMono err(InteractionEnvironment environment, String text, Object... args);

    InteractionApplicationCommandCallbackReplyMono errTitled(InteractionEnvironment environment, String title, String text, Object... args);

    // data

    String encrypt(String text, Snowflake messageId, Snowflake channelId);

    String decrypt(String text, Snowflake messageId, Snowflake channelId);

    // caching

    void awaitEdit(Snowflake messageId);

    void removeEdit(Snowflake messageId);

    boolean isAwaitEdit(Snowflake messageId);
}
