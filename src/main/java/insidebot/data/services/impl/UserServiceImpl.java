package insidebot.data.services.impl;

import arc.Events;
import arc.func.Prov;
import arc.util.Log;
import discord4j.core.object.entity.Member;
import insidebot.EventType;
import insidebot.data.entity.UserInfo;
import insidebot.data.repository.UserInfoRepository;
import insidebot.data.services.UserService;
import org.joda.time.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

import java.util.Objects;

import static insidebot.InsideBot.activeUserRoleID;

@Service
public class UserServiceImpl implements UserService{
    @Autowired
    private UserInfoRepository repository;

    @Autowired
    private Logger log;

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

    @Scheduled(cron = "*/2 * * * *")
    public void unmuteUsers(){
        repository.getAll().filter(i -> i.asMember() != null && isMuteEnd(i))
                  .subscribe(info -> Events.fire(new EventType.MemberUnmuteEvent(info)), Log::err);
    }

    @Scheduled(cron = "* * * * *")
    public void activeUsers(){
        repository.getAll().filterWhen(u -> {
            return u.asMember().map(Objects::nonNull).filterWhen(b -> {
                return b ? Mono.just(true) : Mono.fromRunnable(() -> log.warn("Member '{}' not found", u.name()));
            });
        }).subscribe(u -> {
            Member member = u.asMember().block();
            if(isActiveUser(u)){
                member.addRole(activeUserRoleID).block();
            }else{
                member.removeRole(activeUserRoleID).block();
            }
        }, Log::err);
    }

    protected boolean isMuteEnd(@NonNull UserInfo userInfo){ // что???
        return userInfo.muteEndDate() != null && DateTime.now().isAfter(new DateTime(userInfo.muteEndDate()));
    }

    protected boolean isActiveUser(@NonNull UserInfo userInfo){
        if(userInfo.lastSentMessage() == null) return false;
        DateTime last = new DateTime(userInfo.lastSentMessage());
        int diff = Weeks.weeksBetween(last, DateTime.now()).getWeeks();

        if(diff >= 3){
            userInfo.messageSeq(0);
            save(userInfo);
            return false;
        }else return userInfo.messageSeq() >= 75;
    }
}
