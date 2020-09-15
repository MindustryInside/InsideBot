package insidebot;

import arc.struct.Array;
import arc.util.Log;
import insidebot.data.RowExtractor;
import insidebot.data.RowMapper;
import org.h2.tools.Server;

import javax.annotation.Nonnull;
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
        try {
            Log.info("Connecting to database...");
            Class.forName("org.h2.Driver");
            Server.createTcpServer("-tcpAllowOthers").start();
            con = DriverManager.getConnection(config.get("db-url"), config.get("db-username"), config.get("db-password"));
            init();
        } catch (SQLException | ClassNotFoundException e) {
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

    public Connection getCon() {
        return con;
    }

    // гениально
    public void preparedExecute(String sql, @Nonnull Object... values){
        try (PreparedStatement stmt = getCon().prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                stmt.setObject(i + 1, values[i]);
            }

            stmt.executeUpdate();
        } catch (SQLException e) {
            Log.err(e);
        }
    }

    // хммм
    @Nullable
    public <T> Array<T> preparedQuery(String sql, @Nonnull Object[] args, RowMapper<T> rowMapper){
        try (PreparedStatement statement = getCon().prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                statement.setObject(i + 1, args[i]);
            }

            RowExtractor<T> r = new RowExtractor<>(rowMapper);

            return r.extractData(statement.executeQuery());
        } catch (SQLException e) {
            Log.err(e);
            return null;
        }
    }

    @Nullable
    public <T> Array<T> preparedQuery(String sql, RowMapper<T> rowMapper, @Nonnull Object... args){
        return preparedQuery(sql, args, rowMapper);
    }

    public UserInfo getUserInfo(long id) {
        return preparedQuery("SELECT * FROM DISCORD.USERS_INFO WHERE ID=?;", (rs, rowNum) ->
                new UserInfo(rs.getString("NAME"), id, rs.getLong("LAST_SENT_MESSAGE_ID")), id).first();
    }

    public DateFormat format(){
        return new SimpleDateFormat("MM-dd HH:mm");
    }

    public String zonedFormat(){
        return DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss ZZZZ").format(ZonedDateTime.now());
    }
}
