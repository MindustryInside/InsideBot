package insidebot;

import arc.struct.Array;
import arc.util.Log;
import insidebot.data.RowMapper;
import insidebot.data.ResultSetExtractor;
import org.h2.tools.Server;

import javax.annotation.Nullable;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static insidebot.InsideBot.config;

public class Database{

    private Connection con;

    public Database(){
        try{
            Log.info("Connecting to database...");
            Class.forName("org.h2.Driver");
            Server.createTcpServer("-tcpAllowOthers").start();
            con = DriverManager.getConnection(config.get("db-url"), config.get("db-username"), config.get("db-password"));
            init();
        }catch(SQLException | ClassNotFoundException e){
            Log.err(e);
        }
    }

    private void init() throws SQLException{
        getCon().createStatement().execute(
                "CREATE SCHEMA IF NOT EXISTS DISCORD;"
        );
        getCon().createStatement().execute(
                "CREATE TABLE IF NOT EXISTS DISCORD.USERS_INFO (" +
                "NAME VARCHAR(40), " +
                "ID LONG, " +
                "LAST_SENT_MESSAGE_DATE VARCHAR(20), " +
                "LAST_SENT_MESSAGE_ID LONG, " +
                "MESSAGES_PER_WEEK INT(11), " +
                "WARNS INT(11), " +
                "MUTE_END_DATE VARCHAR(20));"
        );
    }

    public Connection getCon(){
        return con;
    }

    @Nullable
    private PreparedStatement filledStatement(String sql, Object... args){
        try{
            PreparedStatement stmt = getCon().prepareStatement(sql);
            for(int i = 0; i < args.length; i++){
                stmt.setObject(i + 1, args[i]);
            }

            return stmt;
        }catch(SQLException e){
            return null;
        }
    }

    // гениально
    public void execute(String sql, Object... args){
        try(PreparedStatement stmt = filledStatement(sql, args)){
            if(stmt != null){
                stmt.executeUpdate();
            }
        }catch(SQLException e){
            Log.err(e);
        }
    }

    @Nullable
    public <T> Array<T> query(String sql, RowMapper<T> rowMapper, Object... args){
        try(PreparedStatement statement = filledStatement(sql, args)){
            return new ResultSetExtractor<>(rowMapper).extractData(statement.executeQuery());
        }catch(SQLException e){
            return null;
        }
    }

    @Nullable
    public <T> T get(String sql, RowMapper<T> rowMapper, Object... args){
        Array<T> t = query(sql, rowMapper, args);
        return (t != null && !t.isEmpty()) ? t.first() : null;
    }

    public DateFormat format(){
        return new SimpleDateFormat("yyyy-MM-dd HH:mm");
    }

    public String zonedFormat(){
        return DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss ZZZZ").format(ZonedDateTime.now());
    }
}
