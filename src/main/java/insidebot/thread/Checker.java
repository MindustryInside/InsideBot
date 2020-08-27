package insidebot.thread;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Calendar;

import static insidebot.InsideBot.data;

public class Checker extends Thread{
    public Checker(){
        start();
    }

    @Override
    public void run() {
        while (true){
            try {
                Statement statement = data.getCon().createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT MUTE_END_DATE FROM DISCORD.USERS_INFO;");

                while (resultSet.next()) {
                    String end = resultSet.getString("MUTE_END_DATE");

                    if(check(end)){
                        ResultSet resultId = statement.executeQuery("SELECT ID FROM DISCORD.USERS_INFO WHERE MUTE_END_DATE='" + end + "';");

                        long id = 0;
                        while (resultId.next()){
                            id = resultId.getLong(1);
                        }

                        data.getUserInfo(id).unmute();
                    }
                }

                sleep(10000);
            }catch (InterruptedException | SQLException ignored){}
        }
    }

    private boolean check(String time){
        try {
            Calendar unmuteDate = Calendar.getInstance();
            unmuteDate.setTime(data.format().parse(time));
            return LocalDateTime.now().getDayOfYear() >= unmuteDate.get(Calendar.DAY_OF_YEAR);
        }catch (Exception e){
            return false;
        }
    }
}
