package insidebot.thread;

import arc.Events;
import insidebot.EventType.MemberUnmuteEvent;
import insidebot.data.dao.UserInfoDao;
import insidebot.data.model.UserInfo;
import org.joda.time.*;
import reactor.util.annotation.NonNull;

public class Unmuter implements Runnable{

    @Override
    public void run(){
        UserInfoDao.getAll().forEach(info -> {
            if(info.asMember() != null && check(info)){
                Events.fire(new MemberUnmuteEvent(info));
            }
        });
    }

    private boolean check(@NonNull UserInfo userInfo){
        DateTime d = new DateTime(userInfo.getMuteEndDate());
        return userInfo.getMuteEndDate() != null && DateTime.now().isAfter(d);
    }
}
