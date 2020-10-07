package insidebot.thread;

import arc.util.Log;
import insidebot.UserInfo;
import net.dv8tion.jda.api.entities.Member;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Calendar;

import static insidebot.InsideBot.*;

public class ActiveUsers extends Thread{

    public ActiveUsers(){
        start();
    }

    @Override
    public void run(){
        while(true){
            try{
                Statement statement = data.getCon().createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT MESSAGES_PER_WEEK, LAST_SENT_MESSAGE_DATE FROM DISCORD.USERS_INFO;");

                while(resultSet.next()){
                    String lastSendMessage = resultSet.getString("LAST_SENT_MESSAGE_DATE");
                    int messages = resultSet.getInt("MESSAGES_PER_WEEK");
                    PreparedStatement stmt = data.getCon().prepareStatement("SELECT * FROM DISCORD.USERS_INFO WHERE LAST_SENT_MESSAGE_DATE=? AND MESSAGES_PER_WEEK=?;");

                    stmt.setString(1, lastSendMessage);
                    stmt.setInt(2, messages);

                    ResultSet resultId = stmt.executeQuery();

                    long id = 0;
                    while(resultId.next()){
                        id = resultId.getLong("ID");
                    }

                    Member member = listener.guild.getMemberById(id);
                    if(member != null){
                        if(check(lastSendMessage, messages, id)){
                            listener.guild.addRoleToMember(member, listener.jda.getRolesByName(activeUserRoleName, true).get(0)).queue();
                        }else{
                            listener.guild.removeRoleFromMember(member, listener.jda.getRolesByName(activeUserRoleName, true).get(0)).queue();
                        }
                    }
                }

                sleep(60000);
            }catch(InterruptedException | SQLException e){
                Log.err(e);
            }
        }
    }

    private boolean check(String time, int messages, long id){
        try{
            Calendar lastSentMessage = Calendar.getInstance();
            lastSentMessage.setTime(data.format().parse(time));
            int nowWeek = LocalDateTime.now().getDayOfWeek().getValue();
            int lastSentWeek = lastSentMessage.get(Calendar.WEEK_OF_YEAR);

            if(nowWeek - lastSentWeek >= 3){
                UserInfo.get(id).clearQueue();
                return false;
            }else return messages >= 75 && nowWeek - lastSentWeek < 3;
        }catch(Exception e){
            return false;
        }
    }
}
