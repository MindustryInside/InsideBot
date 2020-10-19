package insidebot.thread;

import insidebot.data.dao.UserInfoDao;
import insidebot.data.model.UserInfo;

import java.time.LocalDateTime;
import java.util.Calendar;

import static insidebot.InsideBot.listener;

public class Unmuter implements Runnable{

    @Override
    public void run(){
        for(UserInfo info : UserInfoDao.getAll()){
            if(check(info) && info.asMember() != null){
                listener.onMemberUnmute(info);
            }
        }
    }

    private boolean check(UserInfo userInfo){
        try{
            return LocalDateTime.now().getDayOfYear() > userInfo.getMuteEndDate().get(Calendar.DAY_OF_YEAR);
        }catch(Exception e){
            return false;
        }
    }
}
