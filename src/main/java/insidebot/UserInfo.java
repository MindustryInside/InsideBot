package insidebot;

import arc.util.Log;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Objects;

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

            PreparedStatement statement = data.getCon().prepareStatement(
                "INSERT INTO DISCORD.USERS_INFO (NAME, ID, LAST_SENT_MESSAGE_DATE, LAST_SENT_MESSAGE_ID, MESSAGES_PER_WEEK, WARNS, MUTE_END_DATE) " +
                    "SELECT ?, ?, ?, ?, ?, ?, ? FROM DUAL WHERE NOT EXISTS (SELECT ID FROM DISCORD.USERS_INFO WHERE NAME=? AND ID=?);"
            );

            statement.setString(1, name);
            statement.setLong(2, id);
            statement.setString(3, data.format().format(lastSentMessage.getTime()));
            statement.setLong(4, lastMessageId);
            statement.setInt(5, getMessagesQueue());
            statement.setInt(6, getWarns());
            statement.setString(7, unmuteDate());

            statement.setString(8, name);
            statement.setLong(9, id);

            statement.executeUpdate();
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
            PreparedStatement statement = data.getCon().prepareStatement("SELECT * FROM DISCORD.USERS_INFO WHERE NAME=? AND ID=?;");

            statement.setString(1, name);
            statement.setLong(2, id);

            ResultSet resultSet = statement.executeQuery();

            String date = "";
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

        try {
            PreparedStatement statement = data.getCon().prepareStatement("UPDATE DISCORD.USERS_INFO SET NAME=? WHERE ID=?;");

            statement.setString(1, name);
            statement.setLong(2, id);

            statement.executeUpdate();
        } catch (SQLException e) {
            Log.err(e);
        }
    }

    public void setLastMessageId(long lastMessageId) {
        this.lastMessageId = lastMessageId;
        lastSentMessage = Calendar.getInstance();
        addToQueue();

        try{
            PreparedStatement statement = data.getCon().prepareStatement("UPDATE DISCORD.USERS_INFO SET LAST_SENT_MESSAGE_ID=?, LAST_SENT_MESSAGE_DATE=? WHERE NAME=? AND ID=?;");

            statement.setLong(1, lastMessageId);
            statement.setString(2, data.format().format(lastSentMessage.getTime()));
            statement.setString(3, name);
            statement.setLong(4, id);

            statement.executeUpdate();
        } catch (SQLException e) {
            Log.err(e);
        }
    }

    public void mute(int delayDays) {
        try {
            PreparedStatement statement = data.getCon().prepareStatement("UPDATE DISCORD.USERS_INFO SET MUTE_END_DATE=? WHERE NAME=? AND ID=?;");
            Calendar calendar = Calendar.getInstance();
            calendar.roll(Calendar.DAY_OF_YEAR, +delayDays);

            statement.setString(1, data.format().format(calendar.getTime()));
            statement.setString(2, name);
            statement.setLong(3, id);

            statement.executeUpdate();
            listener.handleAction(jda.retrieveUserById(id).complete(), Listener.ActionType.mute);
        } catch (SQLException e) {
            Log.err(e);
        }
    }

    public void ban(){
        try {
            PreparedStatement statement = data.getCon().prepareStatement("DELETE FROM DISCORD.USERS_INFO WHERE NAME=? AND ID=?;");

            statement.setString(1, name);
            statement.setLong(2, id);

            statement.executeUpdate();
            listener.handleAction(jda.retrieveUserById(id).complete(), Listener.ActionType.ban);
        } catch (SQLException e) {
            Log.err(e);
        }
    }

    public void unmute(){
        try {
            PreparedStatement statement = data.getCon().prepareStatement("UPDATE DISCORD.USERS_INFO SET MUTE_END_DATE=? WHERE NAME=? AND ID=?;");

            statement.setString(1, "");
            statement.setString(2, name);
            statement.setLong(3, id);

            statement.executeUpdate();
            listener.handleAction(jda.retrieveUserById(id).complete(), Listener.ActionType.unMute);
        } catch (SQLException e) {
            Log.err(e);
        }
    }

    public void removeWarns(int count){
        try {
            PreparedStatement statement = data.getCon().prepareStatement("UPDATE DISCORD.USERS_INFO SET WARNS=? WHERE NAME=? AND ID=?;");
            int warns = getWarns() - count;

            statement.setInt(1, warns);
            statement.setString(2, name);
            statement.setLong(3, id);

            statement.executeUpdate();
        } catch (SQLException e) {
            Log.err(e);
        }
    }

    public void addWarns() {
        try {
            PreparedStatement statement = data.getCon().prepareStatement("UPDATE DISCORD.USERS_INFO SET WARNS=? WHERE NAME=? AND ID=?;");

            statement.setInt(1, (getWarns() + 1));
            statement.setString(2, name);
            statement.setLong(3, id);

            statement.executeUpdate();
        } catch (SQLException e) {
            Log.err(e);
        }
    }

    public int getWarns() {
        try {
            PreparedStatement statement = data.getCon().prepareStatement("SELECT * FROM DISCORD.USERS_INFO WHERE NAME=? AND ID=?;");

            statement.setString(1, name);
            statement.setLong(2, id);

            ResultSet resultSet = statement.executeQuery();


            int warns = 0;
            while (resultSet.next()) {
                warns = resultSet.getInt("WARNS");
            }

            return warns;
        } catch (SQLException e) {
            Log.err(e);
            return 0;
        }
    }

    public int getMessagesQueue(){
        try {
            PreparedStatement statement = data.getCon().prepareStatement("SELECT * FROM DISCORD.USERS_INFO WHERE NAME=? AND ID=?;");

            statement.setString(1, name);
            statement.setLong(2, id);

            ResultSet resultSet = statement.executeQuery();

            int queue = 0;
            while (resultSet.next()) {
                queue = resultSet.getInt("MESSAGES_PER_WEEK");
            }

            return queue;
        } catch (SQLException e) {
            Log.err(e);
            return 0;
        }
    }

    private void addToQueue(){
        try {
            PreparedStatement statement = data.getCon().prepareStatement("UPDATE DISCORD.USERS_INFO SET MESSAGES_PER_WEEK=? WHERE NAME=? AND ID=?;");

            statement.setInt(1, getMessagesQueue());
            statement.setString(2, name);
            statement.setLong(3, id);

            statement.executeUpdate();
        } catch (SQLException e) {
            Log.err(e);
        }
    }

    public void clearQueue(){
        try {
            PreparedStatement statement = data.getCon().prepareStatement("UPDATE DISCORD.USERS_INFO SET MESSAGES_PER_WEEK=? WHERE NAME=? AND ID=?;");

            statement.setInt(1, 0);
            statement.setString(2, name);
            statement.setLong(3, id);

            statement.executeUpdate();
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
