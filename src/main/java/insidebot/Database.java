package insidebot;

import arc.util.Log;

import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;

import static insidebot.InsideBot.config;

public class Database {
    private Connection con;

    public Database(){
        try {
            Class.forName("org.h2.Driver");
            con = DriverManager.getConnection(config.get("db-url"), config.get("db-username"), config.get("db-password"));
            Log.info("The database connection is made.");
            init();
        }catch (SQLException | ClassNotFoundException e){
            Log.err(e);
        }
    }

    public void init(){
        try {
            getCon().createStatement().execute(
                "CREATE SCHEMA IF NOT EXISTS DISCORD;"
            );
            getCon().createStatement().execute(
                "CREATE TABLE IF NOT EXISTS DISCORD.WARNINGS (NAME VARCHAR(40), ID LONG, LAST_SENT_MESSAGE_DATE VARCHAR(20), WARNS INT(11), MUTE_END_DATE VARCHAR(20));"
            );
        } catch (SQLException e) {
            Log.info(e);
        }
    }

    public UserInfo getUserInfo(long id){
        return null; // TODO сделать наконец
    }

    public Connection getCon() {
        return con;
    }

    public DateFormat format(){
        return new SimpleDateFormat("MM-dd HH:mm");
    }

    public String nowDate(){
        return String.format("%s-%s %s:%s",
                LocalDateTime.now().getMonthValue(), LocalDateTime.now().getDayOfMonth(),
                LocalDateTime.now().getHour(), LocalDateTime.now().getMinute()
        );
    }
}
