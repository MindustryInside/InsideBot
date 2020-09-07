package insidebot;

import arc.util.Log;

import java.sql.*;
import java.util.Calendar;
import java.util.Objects;

import static insidebot.InsideBot.data;
import static insidebot.InsideBot.listener;

public class UserInfo {

    private String name;
    private final long id;
    private long lastMessageId;
    private Calendar lastSentMessage;

    public UserInfo(String name, long id, long lastMessageId) {
        this.name = name;
        this.id = id;
        this.lastMessageId = lastMessageId;

        lastSentMessage = Calendar.getInstance();

        data.preparedExecute(
            "INSERT INTO DISCORD.USERS_INFO (NAME, ID, LAST_SENT_MESSAGE_DATE, LAST_SENT_MESSAGE_ID, MESSAGES_PER_WEEK, WARNS, MUTE_END_DATE) " +
                "SELECT ?, ?, ?, ?, ?, ?, ? FROM DUAL WHERE NOT EXISTS (SELECT ID FROM DISCORD.USERS_INFO WHERE NAME=? AND ID=?);",
                name, id, data.format().format(lastSentMessage.getTime()), lastMessageId,
                getMessagesQueue(), getWarns(), unmuteDate(), name, id);
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
        // позже и с результ сетом разберусь
        try (PreparedStatement statement = data.getCon().prepareStatement("SELECT * FROM DISCORD.USERS_INFO WHERE NAME=? AND ID=?;")) {

            statement.setString(1, name);
            statement.setLong(2, id);

            ResultSet resultSet = statement.executeQuery();

            String date = "";
            while (resultSet.next()) {
                date = resultSet.getString("MUTE_END_DATE");
            }

            return date;
        } catch (SQLException e) {
            Log.err(e);
            return "";
        }
    }

    public void setName(String name) {
        this.name = name;

        data.preparedExecute("UPDATE DISCORD.USERS_INFO SET NAME=? WHERE ID=?;",
                             name, id);
    }

    public void setLastMessageId(long lastMessageId) {
        this.lastMessageId = lastMessageId;
        lastSentMessage = Calendar.getInstance();
        addToQueue();

        data.preparedExecute("UPDATE DISCORD.USERS_INFO SET LAST_SENT_MESSAGE_ID=?, LAST_SENT_MESSAGE_DATE=? WHERE NAME=? AND ID=?;",
                             lastMessageId, data.format().format(lastSentMessage.getTime()), name, id);
    }

    public void mute(int delayDays) {
        Calendar calendar = Calendar.getInstance();
        calendar.roll(Calendar.DAY_OF_YEAR, +delayDays);

        data.preparedExecute("UPDATE DISCORD.USERS_INFO SET MUTE_END_DATE=? WHERE NAME=? AND ID=?;",
                             data.format().format(calendar.getTime()), name, id);

        listener.handleAction(listener.jda.retrieveUserById(id).complete(), Listener.ActionType.mute);
    }

    public void ban(){
        data.preparedExecute("DELETE FROM DISCORD.USERS_INFO WHERE NAME=? AND ID=?;",
                             name, id);

        listener.handleAction(listener.jda.retrieveUserById(id).complete(), Listener.ActionType.ban);
    }

    public void unmute(){
        data.preparedExecute("UPDATE DISCORD.USERS_INFO SET MUTE_END_DATE=? WHERE NAME=? AND ID=?;",
                      "", name, id);

        listener.handleAction(listener.jda.retrieveUserById(id).complete(), Listener.ActionType.unMute);
    }

    public void removeWarns(int count){
        int warns = getWarns() - count;

        data.preparedExecute("UPDATE DISCORD.USERS_INFO SET WARNS=? WHERE NAME=? AND ID=?;",
                             warns, name, id);
    }

    public void addWarns() {
        data.preparedExecute("UPDATE DISCORD.USERS_INFO SET WARNS=? WHERE NAME=? AND ID=?;",
                             (getWarns() + 1), name, id);
    }

    public int getWarns() {
        try (PreparedStatement statement = data.getCon().prepareStatement("SELECT * FROM DISCORD.USERS_INFO WHERE NAME=? AND ID=?;")) {

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
        try (PreparedStatement statement = data.getCon().prepareStatement("SELECT * FROM DISCORD.USERS_INFO WHERE NAME=? AND ID=?;")) {

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
        data.preparedExecute("UPDATE DISCORD.USERS_INFO SET MESSAGES_PER_WEEK=? WHERE NAME=? AND ID=?;",
                             (getMessagesQueue() + 1), name, id);
    }

    public void clearQueue(){
        data.preparedExecute("UPDATE DISCORD.USERS_INFO SET MESSAGES_PER_WEEK=? WHERE NAME=? AND ID=?;",
                      0, name, id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserInfo userInfo = (UserInfo) o;
        return id == userInfo.id && Objects.equals(name, userInfo.name);
    }
}
