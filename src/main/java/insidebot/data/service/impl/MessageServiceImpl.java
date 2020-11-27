package insidebot.data.service.impl;

import arc.util.Strings;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.*;
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
    public String format(@NonNull String key, Object... args) {
        return context.getMessage(key, args, settings.locale);
    }

    @Override
    public Mono<Void> text(MessageChannel channel, String text, Object... args){
        return channel.createMessage(Strings.format(text, args)).then();
    }

    @Override
    public Mono<Void> info(MessageChannel channel, String title, String text, Object... args){
        return channel.createMessage(s -> s.setEmbed(e -> e.setColor(settings.normalColor).setTitle(title)
                                                           .setDescription(Strings.format(text, args)))).then();
    }

    @Override
    public Mono<Void> err(MessageChannel channel, String text, Object... args){
        return err(channel, get("error"), text, args);
    }

    @Override
    public Mono<Void> err(MessageChannel channel, String title, String text, Object... args){
        return channel.createMessage(s -> s.setEmbed(e -> e.setColor(settings.errorColor).setTitle(title)
                                                           .setDescription(Strings.format(text, args)))).then();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(String messageId){
        return repository.existsById(messageId);
    }

    @Override
    @Transactional(readOnly = true)
    public MessageInfo getById(@NonNull String messageId){
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
    public void deleteById(@NonNull String memberId){
        repository.deleteById(memberId);
    }
}
