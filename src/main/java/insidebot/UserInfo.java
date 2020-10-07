package insidebot;

import net.dv8tion.jda.api.entities.User;

import java.util.Calendar;
import java.util.Objects;

import static insidebot.InsideBot.data;
import static insidebot.InsideBot.listener;

public class UserInfo{

    private final long id;
    private String name;
    private long lastMessageId;
    private Calendar lastSentMessage;

    public UserInfo(long id){
        this.id = id;
    }

    public UserInfo(String name, long id, long lastMessageId){
        this.name = name;
        this.id = id;
        this.lastMessageId = lastMessageId;
    }

    public static UserInfo get(long id){
        UserInfo info = data.get("SELECT * FROM DISCORD.USERS_INFO WHERE ID=?;",
                (rs, rowNum) -> new UserInfo(rs.getString("NAME"), id, rs.getLong("LAST_SENT_MESSAGE_ID")),
                id);

        return info != null ? info : UserInfo.create(id);
    }

    public static UserInfo create(long id){
        User user = listener.jda.retrieveUserById(id).complete();
        if(user == null) throw new NullPointerException("Id isn`t valid");
        UserInfo info = new UserInfo(id);
        data.execute("INSERT INTO DISCORD.USERS_INFO (NAME, ID, LAST_SENT_MESSAGE_DATE, LAST_SENT_MESSAGE_ID, " +
                     "MESSAGES_PER_WEEK, WARNS, MUTE_END_DATE) VALUES (?, ?, ?, ?, ?, ?, ?);",
                user.getName(), id, "", 0L, 0, 0, "");
        return info;
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;

        data.execute("UPDATE DISCORD.USERS_INFO SET NAME=? WHERE ID=?;", name, id);
    }

    public long getId(){
        return id;
    }

    public long getLastMessageId(){
        return lastMessageId;
    }

    public void setLastMessageId(long lastMessageId){
        this.lastMessageId = lastMessageId;
        lastSentMessage = Calendar.getInstance();
        addToQueue();

        data.execute("UPDATE DISCORD.USERS_INFO SET LAST_SENT_MESSAGE_ID=?, LAST_SENT_MESSAGE_DATE=? WHERE ID=?;",
                     lastMessageId, data.format().format(lastSentMessage.getTime()), id);
    }

    public Calendar getLastSentMessage(){
        return lastSentMessage;
    }

    public String unmuteDate(){
        String date = data.get("SELECT * FROM DISCORD.USERS_INFO WHERE ID=?;",
                (rs, rowNum) -> rs.getString("MUTE_END_DATE"), id);
        return date != null ? date : "";
    }

    public void mute(int delayDays){
        Calendar calendar = Calendar.getInstance();
        calendar.roll(Calendar.DAY_OF_YEAR, +delayDays);

        data.execute("UPDATE DISCORD.USERS_INFO SET MUTE_END_DATE=? WHERE NAME=? AND ID=?;",
                     data.format().format(calendar.getTime()), name, id);

        listener.onMemberMute(listener.jda.retrieveUserById(id).complete(), delayDays);
    }

    public void ban(){
        remove();

        listener.guild.ban(listener.jda.retrieveUserById(id).complete(), 0).queue();
    }

    public void remove(){
        data.execute("DELETE FROM DISCORD.USERS_INFO WHERE NAME=? AND ID=?;", name, id);
    }

    public void unmute(){
        data.execute("UPDATE DISCORD.USERS_INFO SET MUTE_END_DATE=? WHERE NAME=? AND ID=?;", "", name, id);

        listener.onMemberUnmute(listener.jda.retrieveUserById(id).complete());
    }

    public void removeWarns(int count){
        int warns = getWarns() - count;

        data.execute("UPDATE DISCORD.USERS_INFO SET WARNS=? WHERE NAME=? AND ID=?;", warns, name, id);
    }

    public void addWarns(){
        data.execute("UPDATE DISCORD.USERS_INFO SET WARNS=? WHERE NAME=? AND ID=?;", (getWarns() + 1), name, id);
    }

    public int getWarns(){
        Integer warns = data.get("SELECT * FROM DISCORD.USERS_INFO WHERE ID=?;",
                (rs, rowNum) -> rs.getInt("WARNS"), id);
        return warns != null ? warns : 0;
    }

    public int getMessagesQueue(){
        Integer queue = data.get("SELECT * FROM DISCORD.USERS_INFO WHERE ID=?;",
                (rs, rowNum) -> rs.getInt("MESSAGES_PER_WEEK"), id);
        return queue != null ? queue : 0;
    }

    private void addToQueue(){
        data.execute("UPDATE DISCORD.USERS_INFO SET MESSAGES_PER_WEEK=? WHERE NAME=? AND ID=?;",
                    (getMessagesQueue() + 1), name, id);
    }

    public void clearQueue(){
        data.execute("UPDATE DISCORD.USERS_INFO SET MESSAGES_PER_WEEK=? WHERE NAME=? AND ID=?;",
                     0, name, id);
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        UserInfo userInfo = (UserInfo) o;
        return id == userInfo.id && Objects.equals(name, userInfo.name);
    }
}
