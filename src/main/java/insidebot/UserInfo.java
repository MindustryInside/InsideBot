package insidebot;

import arc.util.Log;
import net.dv8tion.jda.api.entities.Message;

import java.sql.*;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Objects;

import static insidebot.InsideBot.data;

public class UserInfo {
    private String name;
    private final long id;
    private Message lastMessage;
    private Date lastSentMessage;

    public UserInfo(String name, long id, Message lastMessage) {
        this.name = name;
        this.id = id;
        this.lastMessage = lastMessage;

        try {
            lastSentMessage = InsideBot.data.format().parse(InsideBot.data.nowDate());
            Statement statement = InsideBot.data.getCon().createStatement();

            statement.executeUpdate("INSERT INTO DISCORD.WARNINGS (NAME, ID, LAST_SENT_MESSAGE_DATE, WARNS, MUTE_END_DATE) " +
                    "SELECT '" + name + "', " + id + ", '" + lastSentMessage + "', " + getWarns() + ", '' FROM DUAL " +
                    "WHERE NOT EXISTS (SELECT ID FROM DISCORD.WARNINGS WHERE NAME='" + name + "' AND ID=" + id + " AND LAST_SENT_MESSAGE_DATE='" + lastSentMessage + "');");
        } catch (SQLException | ParseException e) {
            Log.err(e);
        }
    }

    public String getName() {
        return name;
    }

    public long getId() {
        return id;
    }

    public Message getLastMessage() {
        return lastMessage;
    }

    public Date getLastSentMessage() {
        return lastSentMessage;
    }

    public String unmuteDate(){
        try {
            Statement statement = data.getCon().createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT MUTE_END_DATE FROM DISCORD.WARNINGS WHERE NAME='" + name + "' AND ID=" + id + ";");

            String date = null;
            while (resultSet.next()) {
                date =  resultSet.getString("MUTE_END_DATE");
            }
            return date;
        } catch (SQLException e) {
            Log.err(e);
            return null;
        }
    }

    public boolean isMuted() {
        try {
            Statement statement = InsideBot.data.getCon().createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT MUTE_END_DATE WHERE NAME='" + name +"' AND ID=" + id + ";");

            return resultSet.next();
        } catch (SQLException e) {
            Log.err(e);
            return false;
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLastMessage(Message lastMessage) throws ParseException {
        this.lastMessage = lastMessage;
        this.lastSentMessage = InsideBot.data.format().parse(InsideBot.data.nowDate());
    }

    public void mute(int delayDays) {
        try {
            Statement statement = InsideBot.data.getCon().createStatement();

            String date = String.format("%s-%s %s:%s",
                    LocalDateTime.now().getMonthValue() + delayDays, LocalDateTime.now().getDayOfMonth(),
                    LocalDateTime.now().getHour(), LocalDateTime.now().getMinute()
            );

            statement.executeUpdate("UPDATE DISCORD.WARNINGS SET MUTE_END_DATE='" + date + "' WHERE NAME='" + name + "' AND ID=" + id + ";");
        } catch (SQLException e) {
            Log.err(e);
        }
    }

    public void removeWarns(int count){
        try {
            Statement statement = InsideBot.data.getCon().createStatement();
            int warns = getWarns() - count;

            statement.executeUpdate("UPDATE DISCORD.WARNINGS SET WARNS=" + warns + " WHERE NAME='" + name + "' AND ID=" + id + ";");
        } catch (SQLException e) {
            Log.err(e);
        }
    }

    public void addWarns() {
        try {
            Statement statement = InsideBot.data.getCon().createStatement();

            statement.executeUpdate("UPDATE DISCORD.WARNINGS SET WARNS=" + (getWarns() + 1) + " WHERE NAME='" + name + "' AND ID=" + id + ";");
        } catch (SQLException e) {
            Log.err(e);
        }
    }

    public int getWarns() {
        try {
            int warns = 0;
            Statement statement = InsideBot.data.getCon().createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT WARNS FROM DISCORD.WARNINGS WHERE NAME='" + name + "', AND ID=" + id + ";");

            while (resultSet.next()) {
                warns = resultSet.getInt(1);
            }

            return warns;
        } catch (SQLException e) {
            Log.err(e);
            return 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserInfo userInfo = (UserInfo) o;
        return id == userInfo.id &&
                Objects.equals(name, userInfo.name) &&
                Objects.equals(lastMessage, userInfo.lastMessage) &&
                Objects.equals(lastSentMessage, userInfo.lastSentMessage);
    }
}
