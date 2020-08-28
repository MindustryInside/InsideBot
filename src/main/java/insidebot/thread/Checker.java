package insidebot.thread;

import net.dv8tion.jda.api.EmbedBuilder;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

import static insidebot.InsideBot.*;

public class Checker extends Thread{
    public Checker(){
        start();
    }

    @Override
    public void run() {
        while (true){
            try {
                PreparedStatement statement = data.getCon().prepareStatement("SELECT * FROM DISCORD.USERS_INFO;");
                ResultSet resultSet = statement.getResultSet();
                statement.executeUpdate();

                while (resultSet.next()) {
                    String end = resultSet.getString("MUTE_END_DATE");

                    if(check(end)){
                        PreparedStatement stmt = data.getCon().prepareStatement("SELECT ID FROM DISCORD.USERS_INFO WHERE MUTE_END_DATE=?;");

                        stmt.setString(1, end);

                        ResultSet resultId = stmt.getResultSet();
                        stmt.executeUpdate();

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
