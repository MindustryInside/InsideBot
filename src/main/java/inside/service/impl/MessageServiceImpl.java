package inside.service.impl;

import com.github.benmanes.caffeine.cache.*;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.*;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.util.AllowedMentions;
import inside.Settings;
import inside.command.model.CommandEnvironment;
import inside.interaction.InteractionEnvironment;
import inside.service.MessageService;
import inside.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static inside.util.ContextUtil.*;

@Service
public class MessageServiceImpl implements MessageService{

    private static final Map<String, Locale> locales;

    private static final Map<String, Map<String, Pattern>> pluralRules;

    private static final String ruLocale = "ru";
    private static final String defaultLocale = "en";

    static{
        locales = Map.of(
                ruLocale, new Locale(ruLocale),
                defaultLocale, new Locale(defaultLocale)
        );

        pluralRules = Map.of(
                ruLocale, Map.of(
                        "zero", Pattern.compile("^\\d*0$"),
                        "one", Pattern.compile("^(-?\\d*[^1])?1$"),
                        "two", Pattern.compile("^(-?\\d*[^1])?2$"),
                        "few", Pattern.compile("(^(-?\\d*[^1])?3)|(^(-?\\d*[^1])?4)$"),
                        "many", Pattern.compile("^\\d+$")
                ),
                defaultLocale, Map.of(
                        "zero", Pattern.compile("^0$"),
                        "one", Pattern.compile("^1$"),
                        "other", Pattern.compile("^\\d+$")
                )
        );
    }

    private final ApplicationContext context;

    private final Settings settings;

    private final Cache<Snowflake, Boolean> waitingMessage = Caffeine.newBuilder()
            .expireAfterWrite(15, TimeUnit.SECONDS)
            .build();

    public MessageServiceImpl(@Autowired ApplicationContext context,
                              @Autowired Settings settings){
        this.context = context;
        this.settings = settings;
    }

    @Override
    public String get(ContextView ctx, String key){
        try{
            return Strings.isEmpty(key) ? "" : context.getMessage(key, null, ctx.get(KEY_LOCALE));
        }catch(NoSuchMessageException e){
            return key;
        }
    }

    @Override
    public String getPluralized(ContextView ctx, String key, long count){
        String code = getCount0(ctx.get(KEY_LOCALE), count);
        return get(ctx, String.format("%s.%s", key, code));
    }

    private String getCount0(Locale locale, long value){
        String str = String.valueOf(value);
        Map<String, Pattern> rules = pluralRules.getOrDefault(locale.getLanguage(), pluralRules.get(defaultLocale));
        return rules.entrySet().stream()
                .filter(plural -> plural.getValue().matcher(str).find())
                .findFirst()
                .map(Map.Entry::getKey)
                .orElse("other");
    }

    @Override
    public String getEnum(ContextView ctx, Enum<?> type){
        return get(ctx, String.format("%s.%s", type.getClass().getCanonicalName(), type.name()));
    }

    @Override
    public boolean hasEnum(ContextView ctx, Enum<?> type){
        try{
            String code = String.format("%s.%s", type.getClass().getCanonicalName(), type.name());
            context.getMessage(code, null, ctx.get(KEY_LOCALE));
            return true;
        }catch(NoSuchMessageException e){
            return false;
        }
    }

    @Override
    public String format(ContextView ctx, String key, Object... args){
        try{
            return context.getMessage(key, args, ctx.get(KEY_LOCALE));
        }catch(NoSuchMessageException e){
            return key;
        }
    }

    @Override
    public Optional<Locale> getLocale(String str){
        return Optional.ofNullable(locales.get(str));
    }

    @Override
    public Map<String, Locale> getSupportedLocales(){
        return locales;
    }

    @Override
    public Locale getDefaultLocale(){
        return locales.get(defaultLocale);
    }

    @Override
    public MessageCreateMono text(CommandEnvironment environment, String text, Object... args){
        return environment.channel().createMessage(
                text.isBlank() ? get(environment.context(), "message.placeholder")
                        : format(environment.context(), text, args));
    }

    @Override
    public MessageCreateMono info(CommandEnvironment environment, String text, Object... args){
        return environment.channel().createMessage(EmbedCreateSpec.builder()
                .description(format(environment.context(), text, args))
                .color(settings.getDefaults().getNormalColor())
                .build());
    }

    @Override
    public MessageCreateMono infoTitled(CommandEnvironment environment, String title, String text, Object... args){
        return environment.channel().createMessage(EmbedCreateSpec.builder()
                .title(get(environment.context(), title))
                .description(format(environment.context(), text, args))
                .color(settings.getDefaults().getNormalColor())
                .build());
    }

    @Override
    public Mono<Void> err(CommandEnvironment environment, String text, Object... args){
        return errTitled(environment, "message.error.general.title", text, args);
    }

    @Override
    public Mono<Void> errTitled(CommandEnvironment environment, String title, String text, Object... args){
        return environment.channel().createMessage(MessageCreateSpec.builder()
                .addEmbed(EmbedCreateSpec.builder()
                        .color(settings.getDefaults().getErrorColor())
                        .description(format(environment.context(), text, args))
                        .title(get(environment.context(), title))
                        .build())
                .build())
                .flatMap(message -> Mono.delay(settings.getDiscord().getErrorEmbedTtl())
                        .then(message.delete().and(environment.message().addReaction(failed))));
    }

    @Override
    public InteractionApplicationCommandCallbackReplyMono text(InteractionEnvironment environment, String text, Object... args){
        return environment.event().reply()
                .withContent(text.isBlank() ? get(environment.context(), "message.placeholder")
                        : format(environment.context(), text, args));
    }

    @Override
    public InteractionApplicationCommandCallbackReplyMono infoTitled(InteractionEnvironment environment, String title, String text, Object... args){
        return environment.event().reply()
                .withEmbeds(EmbedCreateSpec.builder()
                        .color(settings.getDefaults().getNormalColor())
                        .title(get(environment.context(), title))
                        .description(format(environment.context(), text, args))
                        .build());
    }

    @Override
    public InteractionApplicationCommandCallbackReplyMono info(InteractionEnvironment environment, String text, Object... args){
        return environment.event().reply()
                .withEmbeds(EmbedCreateSpec.builder()
                        .color(settings.getDefaults().getNormalColor())
                        .description(format(environment.context(), text, args))
                        .build());
    }

    @Override
    public InteractionApplicationCommandCallbackReplyMono err(InteractionEnvironment environment, String text, Object... args){
        return errTitled(environment, "message.error.general.title", text, args);
    }

    @Override
    public InteractionApplicationCommandCallbackReplyMono errTitled(InteractionEnvironment environment, String title, String text, Object... args){
        return environment.event().reply()
                .withEphemeral(true)
                .withEmbeds(EmbedCreateSpec.builder()
                        .color(settings.getDefaults().getErrorColor())
                        .description(format(environment.context(), text, args))
                        .title(get(environment.context(), title))
                        .build());
    }

    @Override
    public void awaitEdit(Snowflake messageId){
        waitingMessage.put(messageId, true);
    }

    @Override
    public void removeEdit(Snowflake messageId){
        waitingMessage.invalidate(messageId);
    }

    @Override
    public boolean isAwaitEdit(Snowflake messageId){
        return Boolean.TRUE.equals(waitingMessage.getIfPresent(messageId));
    }

    @Override
    public String encrypt(String text, Snowflake messageId, Snowflake channelId){
        if(settings.getDiscord().isEncryptMessages()){
            String password = messageId.asString();
            String salt = channelId.asString();
            AesEncryptor coder = new AesEncryptor(password, salt);
            return coder.encrypt(text);
        }
        return text;
    }

    @Override
    public String decrypt(String text, Snowflake messageId, Snowflake channelId){
        if(settings.getDiscord().isEncryptMessages()){
            String password = messageId.asString();
            String salt = channelId.asString();
            AesEncryptor coder = new AesEncryptor(password, salt);
            return coder.decrypt(text);
        }
        return text;
    }
}
