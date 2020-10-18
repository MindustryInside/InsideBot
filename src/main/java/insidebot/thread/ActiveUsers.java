package insidebot.thread;

import insidebot.data.dao.UserInfoDao;
import insidebot.data.model.UserInfo;
import net.dv8tion.jda.api.entities.Member;

import java.time.LocalDateTime;
import java.util.Calendar;

import static insidebot.InsideBot.activeUserRole;
import static insidebot.InsideBot.listener;

public class ActiveUsers implements Runnable{

    @Override
    public void run(){
        for(UserInfo info : UserInfoDao.getAll()){
            Member member = info.asMember();
            if(member != null){
                if(check(info)){
                    listener.guild.addRoleToMember(member, activeUserRole).queue();
                }else{
                    listener.guild.removeRoleFromMember(member, activeUserRole).queue();
                }
                UserInfoDao.saveOrUpdate(info);
            }
        }
    }

    private boolean check(UserInfo userInfo){
        try{
            int nowWeek = LocalDateTime.now().getDayOfWeek().getValue();
            int lastSentWeek = userInfo.getLastSentMessage().get(Calendar.WEEK_OF_YEAR);

            if(nowWeek - lastSentWeek >= 3){
                userInfo.setMessageSeq(0);
                return false;
            }else return userInfo.getMessageSeq() >= 75 && nowWeek - lastSentWeek < 3;
        }catch(Exception e){
            return false;
        }
    }
}
