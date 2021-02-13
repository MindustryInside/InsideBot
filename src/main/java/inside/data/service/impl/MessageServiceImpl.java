package inside.data.service.impl;

import arc.util.*;
import com.github.benmanes.caffeine.cache.*;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import inside.Settings;
import inside.data.entity.MessageInfo;
import inside.data.repository.MessageInfoRepository;
import inside.data.service.MessageService;
import inside.util.*;
import org.joda.time.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.*;
import reactor.core.scheduler.Schedulers;
import reactor.util.*;
import reactor.util.context.ContextView;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static inside.util.ContextUtil.KEY_LOCALE;

@Service
public class MessageServiceImpl implements MessageService{
    private static final Logger log = Loggers.getLogger(MessageService.class);

    private final MessageInfoRepository repository;

    private final ApplicationContext context;

    private final Settings settings;

    private final Cache<Snowflake, Boolean> deletedMessage = Caffeine.newBuilder()
            .expireAfterWrite(3, TimeUnit.MINUTES)
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
            return context.getMessage(key, null, ctx.get(KEY_LOCALE));
        }catch(Throwable t){
            return "???" + key + "???";
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
        return context.getMessage(key, args, ctx.get(KEY_LOCALE));
    }

    @Override
    public Mono<Void> text(Mono<? extends MessageChannel> channel, String text){
        return channel.publishOn(Schedulers.boundedElastic())
                .flatMap(c -> c.createMessage(Strings.format(text)))
                .then();
    }

    @Override
    public Mono<Void> info(Mono<? extends MessageChannel> channel, String title, String text){
        return info(channel, embed -> embed.setColor(settings.normalColor).setTitle(title).setDescription(text));
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
                .flatMap(c -> c.createEmbed(e -> e.setColor(settings.errorColor).setTitle(title).setDescription(text)))
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
    @Scheduled(cron = "0 0 */4 * * *")
    public void cleanUp(){
        Flux.fromIterable(repository.findAll())
            .filter(messageInfo -> Weeks.weeksBetween(new DateTime(messageInfo.timestamp()), DateTime.now()).getWeeks() >= 3)
            .subscribe(repository::delete);
    }
}
