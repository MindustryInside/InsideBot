package inside.data.service.impl;

import com.github.benmanes.caffeine.cache.*;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.AllowedMentions;
import inside.Settings;
import inside.data.entity.MessageInfo;
import inside.data.repository.MessageInfoRepository;
import inside.data.service.MessageService;
import inside.util.*;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.*;
import reactor.util.context.ContextView;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static inside.util.ContextUtil.KEY_LOCALE;

@Service
public class MessageServiceImpl implements MessageService{
    private static final Logger log = Loggers.getLogger(MessageService.class);

    private final MessageInfoRepository repository;

    private final ApplicationContext context;

    private final Settings settings;

    private final Cache<Snowflake, Boolean> waitingMessage = Caffeine.newBuilder()
            .expireAfterWrite(15, TimeUnit.SECONDS)
            .build();

    public MessageServiceImpl(@Autowired MessageInfoRepository repository,
                              @Autowired ApplicationContext context,
                              @Autowired Settings settings){
        this.repository = repository;
        this.context = context;
        this.settings = settings;
    }

    @Override
    public String get(ContextView ctx, String key){
        try{
            return MessageUtil.isEmpty(key) ? "" : context.getMessage(key, null, ctx.get(KEY_LOCALE));
        }catch(NoSuchMessageException e){
            return key;
        }
    }

    @Override
    public String getCount(ContextView ctx, String key, long count){
        String code = LocaleUtil.getCount(count, ctx.get(KEY_LOCALE));
        return get(ctx, String.format("%s.%s", key, code));
    }

    @Override
    public String getEnum(ContextView ctx, Enum<?> type){
        return get(ctx, String.format("%s.%s", type.getClass().getCanonicalName(), type.name()));
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
    public String localize(ContextView ctx, Throwable throwable){
        if(ctx.getOrEmpty(KEY_LOCALE).map(locale -> LocaleUtil.getDefaultLocale().equals(locale)).orElse(false)){
            return throwable.getMessage();
        }
        String message = throwable.getMessage() != null ? "." + kebalize(throwable.getMessage()) : "";
        return get(ctx, throwable.getClass().getCanonicalName() + message);
    }

    @Override
    public Mono<Void> text(Mono<? extends MessageChannel> channel, String text, Object... args){
        return Mono.deferContextual(ctx -> channel.publishOn(Schedulers.boundedElastic())
                .flatMap(c -> c.createMessage(spec -> spec.setContent(text.isBlank() ? ":eyes: (?)" : format(ctx, text, args))
                        .setAllowedMentions(AllowedMentions.suppressAll())))
                .then());
    }

    @Override
    public Mono<Void> info(Mono<? extends MessageChannel> channel, String title, String text, Object... args){
        return Mono.deferContextual(ctx -> info(channel, embed -> embed.setTitle(get(ctx, title))
                .setDescription(format(ctx, text, args))));
    }

    @Override
    public Mono<Void> info(Mono<? extends MessageChannel> channel, Consumer<EmbedCreateSpec> embed){
        return channel.publishOn(Schedulers.boundedElastic())
                .flatMap(c -> c.createEmbed(embed.andThen(spec -> spec.setColor(settings.normalColor))))
                .then();
    }

    @Override
    public Mono<Void> err(Mono<? extends MessageChannel> channel, String text, Object... args){
        return error(channel, "message.error.general.title", text, args);
    }

    @Override
    public Mono<Void> error(Mono<? extends MessageChannel> channel, String title, String text, Object... args){
        return Mono.deferContextual(ctx -> channel.publishOn(Schedulers.boundedElastic())
                .flatMap(c -> c.createEmbed(embed -> embed.setColor(settings.errorColor)
                        .setDescription(format(ctx, text, args))
                        .setTitle(get(ctx, title))))
                .flatMap(message -> Mono.delay(Duration.ofSeconds(5)).then(message.delete())));
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
    @Transactional(readOnly = true)
    public boolean exists(Snowflake messageId){
        return repository.existsByMessageId(messageId.asString());
    }

    @Override
    @Transactional(readOnly = true)
    public MessageInfo getById(Snowflake messageId){
        return repository.findByMessageId(messageId.asString());
    }

    @Override
    @Transactional
    public void save(MessageInfo message){
        repository.save(message);
    }

    @Override
    @Transactional
    public void deleteById(Snowflake messageId){
        repository.deleteByMessageId(messageId.asString());
    }

    @Override
    @Transactional
    public void delete(MessageInfo message){
        if(message != null){
            repository.delete(message);
        }
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 */4 * * *")
    public void cleanUp(){
        repository.deleteByTimestampBefore(DateTime.now().minusWeeks(settings.historyExpireWeeks));
    }

    // TODO: move to MessageUtil?
    private String kebalize(String s){
        StringBuilder result = new StringBuilder(s.length() + 1);

        for(int i = 0; i < s.length(); ++i){
            char c = s.charAt(i);
            if(i > 0 && Character.isUpperCase(s.charAt(i)) || Character.isWhitespace(c)){
                result.append('-');
            }else{
                result.append(Character.toLowerCase(c));
            }
        }

        return result.toString();
    }
}
