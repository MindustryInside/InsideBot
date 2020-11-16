package insidebot.thread;

import arc.Events;
import insidebot.EventType.MemberUnmuteEvent;
import insidebot.data.dao.UserInfoDao;
import insidebot.data.model.UserInfo;
import reactor.util.annotation.NonNull;

import java.time.LocalDateTime;
import java.util.Calendar;

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
        return userInfo.getMuteEndDate() != null &&
               LocalDateTime.now().getDayOfYear() > userInfo.getMuteEndDate().get(Calendar.DAY_OF_YEAR);
    }
}
