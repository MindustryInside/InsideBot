package inside.service.impl;

import com.github.benmanes.caffeine.cache.*;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.InteractionCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.AllowedMentions;
import inside.Settings;
import inside.data.entity.MessageInfo;
import inside.data.repository.MessageInfoRepository;
import inside.service.MessageService;
import inside.util.*;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.encrypt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.ContextView;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static inside.util.ContextUtil.KEY_LOCALE;

@Service
public class MessageServiceImpl implements MessageService{

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
            return Strings.isEmpty(key) ? "" : context.getMessage(key, null, ctx.get(KEY_LOCALE));
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
    public Mono<Void> text(Mono<? extends MessageChannel> channel, String text, Object... args){
        return Mono.deferContextual(ctx -> channel.publishOn(Schedulers.boundedElastic())
                .flatMap(c -> c.createMessage(spec -> spec.setContent(text.isBlank() ? placeholder : format(ctx, text, args))
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
                .flatMap(c -> c.createEmbed(embed.andThen(spec -> spec.setColor(settings.getDefaults().getNormalColor()))))
                .then();
    }

    @Override
    public Mono<Void> err(Mono<? extends MessageChannel> channel, String text, Object... args){
        return error(channel, "message.error.general.title", text, args);
    }

    @Override
    public Mono<Void> error(Mono<? extends MessageChannel> channel, String title, String text, Object... args){
        return Mono.deferContextual(ctx -> channel.publishOn(Schedulers.boundedElastic())
                .flatMap(c -> c.createEmbed(embed -> embed.setColor(settings.getDefaults().getErrorColor())
                        .setDescription(format(ctx, text, args))
                        .setTitle(get(ctx, title))))
                .flatMap(message -> Mono.delay(settings.getDiscord().getErrorEmbedTtl()).then(message.delete())));
    }

    @Override
    public Mono<Void> text(InteractionCreateEvent event, String text, Object... args){
        return Mono.deferContextual(ctx -> event.reply(spec -> spec.setAllowedMentions(AllowedMentions.suppressAll())
                .setContent(text.isBlank() ? placeholder : format(ctx, text, args))));
    }

    @Override
    public Mono<Void> info(InteractionCreateEvent event, String title, String text, Object... args){
        return Mono.deferContextual(ctx -> info(event, embed -> embed.setTitle(get(ctx, title))
                .setDescription(format(ctx, text, args))));
    }

    @Override
    public Mono<Void> info(InteractionCreateEvent event, Consumer<EmbedCreateSpec> embed){
        return event.reply(spec -> spec.addEmbed(embed.andThen(embedSpec ->
                embedSpec.setColor(settings.getDefaults().getNormalColor()))));
    }

    @Override
    public Mono<Void> err(InteractionCreateEvent event, String text, Object... args){
        return error(event, "message.error.general.title", text, args);
    }

    @Override
    public Mono<Void> error(InteractionCreateEvent event, String title, String text, Object... args){
        return Mono.deferContextual(ctx -> event.reply(spec -> spec.addEmbed(embed -> embed.setColor(settings.getDefaults().getErrorColor())
                .setDescription(format(ctx, text, args))
                .setTitle(get(ctx, title)))));
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
    public MessageInfo getById(Snowflake messageId){
        return repository.findByMessageId(messageId.asLong());
    }

    @Override
    @Transactional
    public void save(MessageInfo message){
        repository.save(message);
    }

    @Override
    @Transactional
    public void deleteById(Snowflake messageId){
        repository.deleteByMessageId(messageId.asLong());
    }

    @Override
    @Transactional
    public void delete(MessageInfo message){
        repository.delete(message);
    }

    @Override
    public String encrypt(String text, Snowflake messageId, Snowflake channelId){
        if(settings.getDiscord().isEncryptMessages()){
            TextEncryptor encryptor = Encryptors.text(messageId.asString(), channelId.asString());
            return encryptor.encrypt(text);
        }
        return text;
    }

    @Override
    public String decrypt(String text, Snowflake messageId, Snowflake channelId){
        if(settings.getDiscord().isEncryptMessages()){
            TextEncryptor encryptor = Encryptors.text(messageId.asString(), channelId.asString());
            return encryptor.decrypt(text);
        }
        return text;
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 */4 * * *")
    public void cleanUp(){
        repository.deleteByTimestampBefore(DateTime.now().minus(settings.getAudit().getHistoryKeep().toMillis()));
    }
}
