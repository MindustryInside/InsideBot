package insidebot.thread;

import arc.util.Log;
import discord4j.core.object.entity.Member;
import insidebot.data.entity.UserInfo;
import insidebot.data.repository.UserInfoRepository;
import org.joda.time.*;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

import java.util.Objects;

import static insidebot.InsideBot.activeUserRoleID;

public class ActiveUsers implements Runnable{
    @Autowired
    private UserInfoRepository userInfoRepository;

    @Override
    public void run(){
        userInfoRepository.getAll().filterWhen(u -> {
            return u.asMember().map(Objects::nonNull).filterWhen(b -> {
                return b ? Mono.just(true) : Mono.fromRunnable(() -> Log.warn("Member '@' not found", u.name()));
            });
        }).subscribe(u -> {
            Member member = u.asMember().block();
            if(check(u)){
                member.addRole(activeUserRoleID).block();
            }else{
                member.removeRole(activeUserRoleID).block();
            }
        }, Log::err);
    }

    private boolean check(@NonNull UserInfo userInfo){
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
