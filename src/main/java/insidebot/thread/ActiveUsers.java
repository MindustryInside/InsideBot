package insidebot.thread;

import discord4j.core.object.entity.Member;
import insidebot.data.dao.UserInfoDao;
import insidebot.data.model.UserInfo;
import org.joda.time.*;
import reactor.util.annotation.NonNull;

import static insidebot.InsideBot.*;

public class ActiveUsers implements Runnable{
    @Override
    public void run(){
        UserInfoDao.all().subscribe(info -> { // todo сделать проверку полностью на реакторе
            Member member = info.asMember();
            if(member != null){
                if(check(info)){
                    member.addRole(activeUserRoleID).block();
                }else{
                    member.removeRole(activeUserRoleID).block();
                }
            }
        });
    }

    private boolean check(@NonNull UserInfo userInfo){
        if(userInfo.getLastSentMessage() == null) return false;
        DateTime last = new DateTime(userInfo.getLastSentMessage());
        int diff = Weeks.weeksBetween(last, DateTime.now()).getWeeks();

        if(diff >= 3){
            userInfo.setMessageSeq(0);
            UserInfoDao.update(userInfo);
            return false;
        }else return userInfo.getMessageSeq() >= 75;
    }
}
