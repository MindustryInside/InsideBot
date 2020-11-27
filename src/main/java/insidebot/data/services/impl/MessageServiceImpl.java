package insidebot.data.services.impl;

import arc.util.Strings;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.TextChannel;
import insidebot.*;
import insidebot.data.entity.*;
import insidebot.data.repository.MessageInfoRepository;
import insidebot.data.services.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    public void text(String text, Object... args){

    }

    @Override
    public void info(String title, String text, Object... args){

    }

    @Override
    public void err(String text, Object... args){

    }

    @Override
    public void err(String title, String text, Object... args){

    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(String messageId){
        return repository.existsById(messageId);
    }

    @Override
    @Transactional(readOnly = true)
    public MessageInfo getById(@NonNull String messageId) {
        return repository.findByMessageId(messageId);
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
