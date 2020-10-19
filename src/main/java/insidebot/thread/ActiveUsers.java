package insidebot.thread;

import insidebot.data.dao.UserInfoDao;
import insidebot.data.model.UserInfo;
import net.dv8tion.jda.api.entities.Member;
import org.joda.time.DateTime;
import org.joda.time.Weeks;

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
            DateTime last = new DateTime(userInfo.getLastSentMessage());
            int diff = Weeks.weeksBetween(last, DateTime.now()).getWeeks();

            if(diff >= 3){
                userInfo.setMessageSeq(0);
                return false;
            }else return userInfo.getMessageSeq() >= 75;
        }catch(Exception e){
            return false;
        }
    }
}
