package inside.data.service.impl;

import arc.util.*;
import com.google.common.cache.*;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import inside.Settings;
import inside.data.entity.MessageInfo;
import inside.data.repository.MessageInfoRepository;
import inside.data.service.MessageService;
import inside.util.*;
import org.joda.time.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.*;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.ContextView;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
public class MessageServiceImpl implements MessageService{
    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    @Autowired
    private MessageInfoRepository repository;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Settings settings;

    private final Cache<Snowflake, Boolean> deletedMessage = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    @Override
    public String get(ContextView ctx, String key){
        try{
            return context.getMessage(key, null, ctx.get(ContextUtil.KEY_LOCALE));
        }catch(Throwable t){
            return "???" + key + "???";
        }
    }

    @Override
    public String getCount(ContextView ctx, String key, long count){
        String code = LocaleUtil.getCount(count, LocaleUtil.getDefaultLocale());
        return get(ctx, String.format("%s.%s", key, code));
    }

    @Override
    public String getEnum(ContextView ctx, Enum<?> type){
        return get(ctx, String.format("%s.%s", type.getClass().getName(), type.name()));
    }

    @Override
    public String format(ContextView ctx, String key, Object... args){
        return context.getMessage(key, args, ctx.get(ContextUtil.KEY_LOCALE));
    }

    @Override
    public Mono<Void> text(Mono<? extends MessageChannel> channel, String text){
        return channel.publishOn(Schedulers.boundedElastic())
                .flatMap(c -> c.createMessage(Strings.format(text)))
                .then();
    }

    @Override
    public Mono<Void> info(Mono<? extends MessageChannel> channel, String title, String text){
        return info(channel, e -> e.setColor(settings.normalColor).setTitle(title).setDescription(text));
    }

    @Override
    public Mono<Void> info(Mono<? extends MessageChannel> channel, Consumer<EmbedCreateSpec> embed){
        return channel.publishOn(Schedulers.boundedElastic())
                .flatMap(c -> c.createEmbed(embed))
                .then();
    }

    @Override
    public Mono<Void> err(Mono<? extends MessageChannel> channel, String text){
        return Mono.deferContextual(ctx -> err(channel, get(ctx, "message.error.general.title"), text));
    }

    @Override
    public Mono<Void> err(Mono<? extends MessageChannel> channel, String title, String text){
        return channel.publishOn(Schedulers.boundedElastic())
                .flatMap(c -> c.createEmbed(e -> e.setColor(settings.errorColor).setTitle(title)
                        .setDescription(text)))
                .then();
    }

    @Override
    public boolean isCleared(Snowflake messageId){
        return Boolean.TRUE.equals(deletedMessage.getIfPresent(messageId));
    }

    @Override
    public void putMessage(Snowflake messageId){
        deletedMessage.put(messageId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(Snowflake messageId){
        return repository.existsByMessageId(messageId);
    }

    @Override
    @Transactional(readOnly = true)
    public MessageInfo getById(Snowflake messageId){
        return repository.findByMessageId(messageId);
    }

    @Override
    @Transactional
    public MessageInfo save(MessageInfo user) {
        return repository.save(user);
    }

    @Override
    @Transactional
    public void delete(MessageInfo message){
        repository.delete(message);
    }

    @Override
    @Transactional
    public void deleteById(Snowflake messageId){
        MessageInfo message = getById(messageId);
        if(message != null){
            repository.delete(message);
        }else{
            log.warn("Message with id '{}' not found", messageId.asString());
        }
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 */2 * * *") // каждые 2 часа
    public void cleanUp(){
        long pre = repository.count();
        log.info("Audit cleanup started...");
        Flux.fromIterable(repository.findAll())
            .filter(m -> Weeks.weeksBetween(new DateTime(m.timestamp()), DateTime.now()).getWeeks() >= 4)
            .subscribe(repository::delete, Log::err, () -> log.info("Audit cleanup finished, deleted {}", pre - repository.count()));
    }
}
