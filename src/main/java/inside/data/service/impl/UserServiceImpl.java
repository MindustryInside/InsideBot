package inside.data.service.impl;

import discord4j.common.util.Snowflake;
import inside.data.entity.LocalUser;
import inside.data.repository.LocalUserRepository;
import inside.data.service.UserService;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Service
public class UserServiceImpl implements UserService{
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private LocalUserRepository repository;

    private final Object $lock = new Object[0];

    @Override
    @Transactional(readOnly = true)
    public LocalUser get(Snowflake userId){
        return repository.findByUserId(userId);
    }

    @Override
    @Transactional
    public LocalUser getOr(Snowflake userId, Supplier<LocalUser> prov){
        LocalUser localUser = get(userId);
        if(localUser == null){
            synchronized($lock){
                localUser = get(userId);
                if(localUser == null){
                    localUser = repository.saveAndFlush(prov.get());
                }
            }
        }
        return localUser;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(Snowflake userId){
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
