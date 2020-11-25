package insidebot.thread;

import arc.Events;
import arc.util.Log;
import insidebot.EventType.MemberUnmuteEvent;
import insidebot.data.entity.UserInfo;
import insidebot.data.repository.UserInfoRepository;
import org.joda.time.*;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.util.annotation.NonNull;

public class Unmuter implements Runnable{
    @Autowired
    private UserInfoRepository userInfoRepository;

    @Override
    public void run(){
        userInfoRepository.getAll().filter(i -> i.asMember() != null && check(i)).subscribe(info -> Events.fire(new MemberUnmuteEvent(info)), Log::err);
    }

    private boolean check(@NonNull UserInfo userInfo){ // что???
        DateTime d = new DateTime(userInfo.muteEndDate());
        return userInfo.muteEndDate() != null && DateTime.now().isAfter(d);
    }
}
