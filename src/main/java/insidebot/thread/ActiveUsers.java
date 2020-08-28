package insidebot.thread;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Calendar;

import static insidebot.InsideBot.*;

public class ActiveUsers extends Thread{
    public ActiveUsers(){
        start();
    }

    @Override
    public void run() {
        while (true){
            try {
                Statement statement = data.getCon().createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT MESSAGES_PER_WEEK, LAST_SENT_MESSAGE_DATE FROM DISCORD.USERS_INFO;");

                while (resultSet.next()) {
                    String lastSendMessage = resultSet.getString("LAST_SENT_MESSAGE_DATE");
                    int messages = resultSet.getInt("MESSAGES_PER_WEEK");

                    ResultSet resultId = statement.executeQuery("SELECT ID FROM DISCORD.USERS_INFO WHERE LAST_SENT_MESSAGE_DATE='" + lastSendMessage + "' AND MESSAGES_PER_WEEK=" + messages + ";");

                    long id = 0;
                    while (resultId.next()){
                        id = resultId.getLong(1);
                    }

                    if(check(lastSendMessage, messages)){
                        listener.guild.addRoleToMember(listener.guild.getMemberById(id), jda.getRolesByName(activeUserRoleName, true).get(0)).queue();
                    }else{
                        listener.guild.removeRoleFromMember(listener.guild.getMemberById(id), jda.getRolesByName(activeUserRoleName, true).get(0)).queue();
                    }
                    data.getUserInfo(id).clearQueue();
                }

                sleep(60000);
            }catch (InterruptedException | SQLException ignored){}
        }
    }

    private boolean check(String time, int messages){
        try {
            Calendar unmuteDate = Calendar.getInstance();
            unmuteDate.setTime(data.format().parse(time));
            return LocalDateTime.now().getDayOfWeek().getValue() >= unmuteDate.get(Calendar.DAY_OF_WEEK) && messages >= 15;
        }catch (Exception e){
            return false;
        }
    }
}
