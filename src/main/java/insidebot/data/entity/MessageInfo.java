package insidebot.data.entity;

import discord4j.common.util.Snowflake;
import insidebot.data.entity.base.GuildEntity;
import reactor.util.annotation.NonNull;

import javax.persistence.*;
import java.util.Calendar;

@Entity
@Table(name = "message_info", schema = "public")
public class MessageInfo extends GuildEntity{
    private static final long serialVersionUID = -7977287922184407665L;

    @Column(name = "channel_id")
    private String channelId;

    @NonNull
    @Column(length = 2000)
    private String content;

    @NonNull
    @Column
    private Calendar timestamp;

    @NonNull
    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserInfo user;

    @NonNull
    public Snowflake id(){
        return Snowflake.of(id);
    }

    public void id(@NonNull Snowflake id){
        this.id = id.asString();
    }

    @NonNull
    public Snowflake guildId(){
        return Snowflake.of(guildId);
    }

    public void guildId(@NonNull Snowflake guildId){
        this.id = guildId.asString();
    }

    @NonNull
    public Snowflake channelId(){
        return Snowflake.of(channelId);
    }

    public void channelId(@NonNull Snowflake channelId){
        this.channelId = channelId.asString();
    }

    @NonNull
    public String content(){
        return content;
    }

    public void content(@NonNull String content){
        this.content = content;
    }

    @NonNull
    public Calendar timestamp(){
        return timestamp;
    }

    public void timestamp(@NonNull Calendar timestamp){
        this.timestamp = timestamp;
    }

    @NonNull
    public UserInfo user(){
        return user;
    }

    public void user(@NonNull UserInfo user){
        this.user = user;
    }

    @Override
    public String toString(){
        return "MessageInfo{" +
               "channelId='" + channelId + '\'' +
               ", content='" + content + '\'' +
               ", timestamp=" + timestamp +
               ", user=" + user +
               ", guildId='" + guildId + '\'' +
               ", id='" + id + '\'' +
               '}';
    }
}
