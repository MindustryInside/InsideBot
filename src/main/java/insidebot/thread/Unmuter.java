package insidebot.thread;

import arc.Events;
import arc.util.Log;
import insidebot.EventType.MemberUnmuteEvent;
import insidebot.data.dao.UserInfoDao;
import insidebot.data.model.UserInfo;
import org.joda.time.*;
import reactor.util.annotation.NonNull;

public class Unmuter implements Runnable{
    @Override
    public void run(){
        UserInfoDao.all()
                   .filter(i -> i.asMember() != null && check(i))
                   .subscribe(info -> Events.fire(new MemberUnmuteEvent(info)), Log::err);
    }

    private boolean check(@NonNull UserInfo userInfo){
        DateTime d = new DateTime(userInfo.getMuteEndDate());
        return userInfo.getMuteEndDate() != null && DateTime.now().isAfter(d);
    }
}
