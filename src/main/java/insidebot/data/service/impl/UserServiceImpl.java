package insidebot.data.service.impl;

import discord4j.common.util.Snowflake;
import insidebot.data.entity.LocalUser;
import insidebot.data.repository.LocalUserRepository;
import insidebot.data.service.UserService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Service
public class UserServiceImpl implements UserService{
    @Autowired
    private LocalUserRepository repository;

    @Autowired
    private Logger log;

    @Override
    @Transactional(readOnly = true)
    public LocalUser get(Snowflake userId){
        log.info("id {}", userId);
        return repository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public LocalUser getOr(Snowflake userId, Supplier<LocalUser> prov){
        log.info("id {}", userId);
        return exists(userId) ? get(userId) : prov.get();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(Snowflake userId){
        log.info("id {}", userId);
        return repository.existsById(userId.asString());
    }

    @Override
    @Transactional
    public LocalUser save(LocalUser user){
        return repository.save(user);
    }

    @Override
    @Transactional
    public void delete(LocalUser user){
        repository.delete(user);
    }

    @Override
    @Transactional
    public void deleteById(Snowflake userId){
        repository.deleteById(userId.asString());
    }
}
