package insidebot.data.service.impl;

import insidebot.data.entity.LocalUser;
import insidebot.data.repository.LocalUserRepository;
import insidebot.data.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.util.annotation.NonNull;

import java.util.function.Supplier;

@Service
public class UserServiceImpl implements UserService{
    @Autowired
    private LocalUserRepository repository;

    @Override
    @Transactional(readOnly = true)
    public LocalUser get(@NonNull LocalUser user){
        return getById(user.userId());
    }

    @Override
    @Transactional
    public LocalUser getOr(String userId, Supplier<LocalUser> prov){
        return exists(userId) ? getById(userId) : prov.get();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(String userId){
        return repository.existsById(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public LocalUser getById(@NonNull String userId){
        return repository.findByUserId(userId);
    }

    @Override
    @Transactional
    public LocalUser save(@NonNull LocalUser user){
        return repository.save(user);
    }

    @Override
    @Transactional
    public void delete(@NonNull LocalUser user){
        repository.delete(user);
    }

    @Override
    @Transactional
    public void deleteById(@NonNull String userId){
        repository.deleteById(userId);
    }
}
