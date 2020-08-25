package insidebot;

import java.sql.*;
import java.time.LocalDateTime;

import static insidebot.InsideBot.data;
import static insidebot.InsideBot.listener;

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
                        listener.unMute(id);
                        statement.execute("DELETE FROM DISCORD.WARNINGS WHERE MUTE_END_DATE='" + end + "';");
                    }
                }

                sleep(10000);
            }catch (InterruptedException | SQLException ignored){}
        }
    }

    // да я знаю что это говно, но на пока сойдёт
    public boolean check(String time){
        try {
            long timeSum = Integer.parseInt(time.replaceAll("-", ""));
            long currentSum = Integer.parseInt(String.format("%s%s%s", LocalDateTime.now().getDayOfMonth(), LocalDateTime.now().getHour(), LocalDateTime.now().getMinute()));
            return currentSum >= timeSum;
        }catch (Exception e){
            return false;
        }
    }
}
