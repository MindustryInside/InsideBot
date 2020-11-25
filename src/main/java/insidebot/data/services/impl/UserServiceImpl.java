package insidebot.data.services.impl;

import arc.func.Prov;
import insidebot.data.entity.UserInfo;
import insidebot.data.repository.UserInfoRepository;
import insidebot.data.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.util.annotation.NonNull;

@Service
public class UserServiceImpl implements UserService{
    @Autowired
    private UserInfoRepository repository;

    @Override
    @Transactional(readOnly = true)
    public UserInfo get(@NonNull UserInfo user){
        return getById(user.userId());
    }

    @Override
    @Transactional
    public UserInfo getOr(String userId, Prov<UserInfo> prov){
        return exists(userId) ? getById(userId) : prov.get();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(String userId){
        return repository.existsById(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserInfo getById(@NonNull String userId){
        return repository.findByUserId(userId);
    }

    @Override
    @Transactional
    public UserInfo save(@NonNull UserInfo user){
        return repository.save(user);
    }

    @Override
    @Transactional
    public void delete(@NonNull UserInfo user){
        repository.delete(user);
    }

    @Override
    @Transactional
    public void deleteById(@NonNull String userId){
        repository.deleteById(userId);
    }
}
