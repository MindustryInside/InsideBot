package insidebot;

import arc.Events;
import arc.util.Log;
import discord4j.core.object.entity.Member;
import insidebot.data.entity.UserInfo;
import insidebot.data.repository.UserInfoRepository;
import org.joda.time.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

import java.util.Objects;

import static insidebot.InsideBot.activeUserRoleID;

// Хм, может стоит переместить?
@Service
public class ActionService{
    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private Logger log;

    @Scheduled(cron = "*/2 * * * *")
    public void unmuteUsers(){
        userInfoRepository.getAll().filter(i -> i.asMember() != null && isMuteEnd(i))
                          .subscribe(info -> Events.fire(new EventType.MemberUnmuteEvent(info)), Log::err);
    }

    @Scheduled(cron = "* * * * *")
    public void activeUsers(){
        userInfoRepository.getAll().filterWhen(u -> {
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
            userInfoRepository.save(userInfo);
            return false;
        }else return userInfo.messageSeq() >= 75;
    }
}
