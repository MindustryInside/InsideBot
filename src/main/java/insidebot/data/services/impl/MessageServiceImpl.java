package insidebot.data.services.impl;

import insidebot.data.entity.*;
import insidebot.data.repository.MessageInfoRepository;
import insidebot.data.services.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.util.annotation.NonNull;

@Service
public class MessageServiceImpl implements MessageService{
    @Autowired
    private MessageInfoRepository repository;

    @Override
    @Transactional(readOnly = true)
    public MessageInfo get(@NonNull MessageInfo message) {
        return getById(message.id());
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
