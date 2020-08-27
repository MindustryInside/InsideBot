package insidebot;

import arc.util.Log;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

import static insidebot.InsideBot.*;

public class UserInfo {
    private String name;
    private final long id;
    private long lastMessageId;
    private Calendar lastSentMessage;

    public UserInfo(String name, long id, long lastMessageId) {
        this.name = name;
        this.id = id;
        this.lastMessageId = lastMessageId;

        try {
            lastSentMessage = Calendar.getInstance();

            addToQueue();

            Statement statement = InsideBot.data.getCon().createStatement();
            statement.executeUpdate(
                    "INSERT INTO DISCORD.USERS_INFO (NAME, ID, LAST_SENT_MESSAGE_DATE, LAST_SENT_MESSAGE_ID, MESSAGES_PER_WEEK, WARNS, MUTE_END_DATE) " +
                    "SELECT '" + name + "', " + id + ", '" + data.format().format(lastSentMessage.getTime()) + "', " + lastMessageId + ", " + getMessagesQueue() + ", " + getWarns() + ", '" + unmuteDate() + "' FROM DUAL " +
                    "WHERE NOT EXISTS (SELECT ID FROM DISCORD.USERS_INFO WHERE NAME='" + name + "' AND ID=" + id + ");"
            );
        } catch (SQLException e) {
            Log.err(e);
        }
    }

    public String getName() {
        return name;
    }

    public long getId() {
        return id;
    }

    public long getLastMessageId() {
        return lastMessageId;
    }

    public Calendar getLastSentMessage() {
        return lastSentMessage;
    }

    public String unmuteDate(){
        try {
            Statement statement = data.getCon().createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT MUTE_END_DATE FROM DISCORD.USERS_INFO WHERE NAME='" + name + "' AND ID=" + id + ";");

            String date = null;
            while (resultSet.next()) {
                date =  resultSet.getString("MUTE_END_DATE");
            }
            return date;
        } catch (SQLException e) {
            Log.err(e);
            return "";
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLastMessageId(long lastMessageId) {
        this.lastMessageId = lastMessageId;
        lastSentMessage = Calendar.getInstance();
    }

    public void mute(int delayDays) {
        try {
            Statement statement = data.getCon().createStatement();
            Calendar calendar = Calendar.getInstance();
            calendar.roll(Calendar.DAY_OF_YEAR, +delayDays);

            statement.executeUpdate("UPDATE DISCORD.USERS_INFO SET MUTE_END_DATE='" + data.format().format(calendar.getTime()) + "' WHERE NAME='" + name + "' AND ID=" + id + ";");
            listener.handleAction(jda.retrieveUserById(id).complete(), Listener.ActionType.mute);
        } catch (SQLException e) {
            Log.err(e);
        }
    }

    public void ban(){
        try {
            Statement statement = data.getCon().createStatement();

            statement.execute("DELETE FROM DISCORD.USERS_INFO WHERE NAME='" +name + "' AND ID=" + id + ";");
            listener.handleAction(jda.retrieveUserById(id).complete(), Listener.ActionType.ban);
        } catch (SQLException e) {
            Log.err(e);
        }
    }

    public void unmute(){
        try {
            Statement statement = data.getCon().createStatement();
            statement.executeUpdate("UPDATE DISCORD.USERS_INFO SET MUTE_END_DATE='' WHERE NAME='" + name + "' AND ID=" + id + ";");

            listener.handleAction(jda.retrieveUserById(id).complete(), Listener.ActionType.unMute);
        } catch (SQLException e) {
            Log.err(e);
        }
    }

    public void removeWarns(int count){
        try {
            Statement statement = data.getCon().createStatement();
            int warns = getWarns() - count;

            statement.executeUpdate("UPDATE DISCORD.USERS_INFO SET WARNS=" + warns + " WHERE NAME='" + name + "' AND ID=" + id + ";");
        } catch (SQLException e) {
            Log.err(e);
        }
    }

    public void addWarns() {
        try {
            Statement statement = data.getCon().createStatement();

            statement.executeUpdate("UPDATE DISCORD.USERS_INFO SET WARNS=" + (getWarns() + 1) + " WHERE NAME='" + name + "' AND ID=" + id + ";");
        } catch (SQLException e) {
            Log.err(e);
        }
    }

    public int getWarns() {
        try {
            Statement statement = data.getCon().createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT WARNS FROM DISCORD.USERS_INFO WHERE NAME='" + name + "' AND ID=" + id + ";");

            int warns = 0;
            while (resultSet.next()) {
                warns = resultSet.getInt(1);
            }

            return warns;
        } catch (SQLException e) {
            Log.err(e);
            return 0;
        }
    }

    public int getMessagesQueue(){
        try {
            Statement statement = data.getCon().createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT MESSAGES_PER_WEEK FROM DISCORD.USERS_INFO WHERE NAME='" + name + "' AND ID=" + id + ";");

            int queue = 0;
            while (resultSet.next()) {
                queue = resultSet.getInt(1);
            }

            return queue;
        } catch (SQLException e) {
            Log.err(e);
            return 0;
        }
    }

    public void addToQueue(){
        try {
            Statement statement = data.getCon().createStatement();

            if(LocalDateTime.now().getDayOfWeek().getValue() < getLastSentMessage().get(Calendar.DAY_OF_WEEK) && getMessagesQueue() <= 14){
                listener.actionGuild.removeRoleFromMember(listener.actionGuild.getMemberById(id), jda.getRolesByName(activeUserRoleName, true).get(0));
                clearQueue();
            }else if(LocalDateTime.now().getDayOfWeek().getValue() >= getLastSentMessage().get(Calendar.DAY_OF_WEEK) && getMessagesQueue() >= 14) {
                listener.actionGuild.addRoleToMember(listener.actionGuild.getMemberById(id), jda.getRolesByName(activeUserRoleName, true).get(0));
                clearQueue();
            }

            statement.executeUpdate("UPDATE DISCORD.USERS_INFO SET MESSAGES_PER_WEEK=" + (getMessagesQueue() + 1) + " WHERE NAME='" + name + "' AND ID=" + id + ";");
        } catch (SQLException e) {
            Log.err(e);
        }
    }

    private void clearQueue(){
        try {
            Statement statement = data.getCon().createStatement();

            statement.executeUpdate("UPDATE DISCORD.USERS_INFO SET MESSAGES_PER_WEEK=0 WHERE NAME='" + name + "' AND ID=" + id + ";");
        } catch (SQLException e) {
            Log.err(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserInfo userInfo = (UserInfo) o;
        return id == userInfo.id && Objects.equals(name, userInfo.name);
    }
}
