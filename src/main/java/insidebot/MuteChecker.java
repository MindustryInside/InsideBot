package insidebot;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Date;

import static insidebot.InsideBot.*;

public class MuteChecker extends Thread{
    public MuteChecker(){
        start();
    }

    @Override
    public void run() {
        while (true){
            try {
                Statement statement = data.getCon().createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT MUTE_END_DATE FROM DISCORD.WARNINGS;");

                while (resultSet.next()) {
                    String end = resultSet.getString("MUTE_END_DATE");

                    if(check(end)){
                        ResultSet resultId = statement.executeQuery("SELECT ID FROM DISCORD.WARNINGS WHERE MUTE_END_DATE='" + end + "';");
                        long id = 0;
                        while (resultId.next()){
                            id = resultId.getLong(1);
                        }
                        listener.handleAction(jda.retrieveUserById(id).complete(), Listener.ActionType.unMute);
                        statement.execute("DELETE FROM DISCORD.WARNINGS WHERE MUTE_END_DATE='" + end + "';");
                    }
                }

                sleep(10000);
            }catch (InterruptedException | SQLException ignored){}
        }
    }

    public boolean check(String time){
        try {
            Date checkTime = data.format().parse(time);
            Date nowTime = data.format().parse(data.nowDate());
            return nowTime.getTime() >= checkTime.getTime();
        }catch (Exception e){
            return false;
        }
    }
}
