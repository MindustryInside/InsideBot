package insidebot.data.service.impl;

import arc.util.Strings;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.MessageChannel;
import insidebot.Settings;
import insidebot.data.entity.MessageInfo;
import insidebot.data.repository.MessageInfoRepository;
import insidebot.data.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

import java.util.Locale;

@Service
public class MessageServiceImpl implements MessageService{
    @Autowired
    private MessageInfoRepository repository;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Settings settings;

    @Override
    public String get(@NonNull String key) {
        return context.getMessage(key, null, settings.locale);
    }

    @Override
    public String get(String key, Locale locale){
        return context.getMessage(key, null, locale);
    }

    @Override
    public String format(@NonNull String key, Object... args) {
        return context.getMessage(key, args, settings.locale);
    }

    @Override
    public String format(String key, Locale locale, Object... args){
        return context.getMessage(key, args, locale);
    }

    @Override
    public Mono<Void> text(MessageChannel channel, String text, Object... args){
        channel.createMessage(Strings.format(text, args)).block();
        return Mono.empty();
    }

    @Override
    public Mono<Void> info(MessageChannel channel, String title, String text, Object... args){
        channel.createMessage(s -> s.setEmbed(e -> e.setColor(settings.normalColor).setTitle(title)
                                                    .setDescription(Strings.format(text, args)))).block();
        return Mono.empty();
    }

    @Override
    public Mono<Void> err(MessageChannel channel, String text, Object... args){
        return err(channel, get("error"), text, args);
    }

    @Override
    public Mono<Void> err(MessageChannel channel, String title, String text, Object... args){
        channel.createMessage(s -> s.setEmbed(e -> e.setColor(settings.errorColor).setTitle(title)
                                                    .setDescription(Strings.format(text, args)))).block();
        return Mono.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(Snowflake messageId){
        return repository.existsById(messageId.asString());
    }

    @Override
    @Transactional(readOnly = true)
    public MessageInfo getById(@NonNull Snowflake messageId){
        return repository.findById(messageId).orElse(null);
    }

    @Override
    @Transactional
    public MessageInfo save(@NonNull MessageInfo user) {
        return repository.save(user);
    }

    @Override
    @Transactional
    public void delete(@NonNull MessageInfo message){
        repository.delete(message);
    }

    @Override
    @Transactional
    public void deleteById(@NonNull Snowflake memberId){
        repository.deleteById(memberId.asString());
    }
}
