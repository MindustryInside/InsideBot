package insidebot.thread;

import insidebot.data.dao.UserInfoDao;
import insidebot.data.model.UserInfo;

import java.time.LocalDateTime;
import java.util.Calendar;

import static insidebot.InsideBot.listener;

public class Unmuter implements Runnable{

    @Override
    public void run(){
        UserInfoDao.getAll().forEach(info -> {
            if(info.asMember() != null && check(info)){
                listener.onMemberUnmute(info);
            }
        });
    }

    private boolean check(UserInfo userInfo){
        return userInfo.getMuteEndDate() != null && LocalDateTime.now().getDayOfYear() > userInfo.getMuteEndDate().get(Calendar.DAY_OF_YEAR);
    }
}
