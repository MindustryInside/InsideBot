package insidebot.data.entity;

import discord4j.common.util.Snowflake;
import insidebot.data.entity.base.GuildEntity;
import reactor.util.annotation.NonNull;

import javax.persistence.*;
import java.util.Calendar;

@Entity
@Table(name = "message_info")
public class MessageInfo extends GuildEntity{
    private static final long serialVersionUID = -7977287922184407665L;

    @Column(name = "channel_id")
    private String channelId;

    @Column(name = "user_id")
    private String userId;

    @Column(length = 2000)
    private String content;

    @Column
    private Calendar timestamp;

    @NonNull
    public Snowflake channelId(){
        return Snowflake.of(channelId);
    }

    public void channelId(@NonNull Snowflake channelId){
        this.channelId = channelId.asString();
    }

    @NonNull
    public Snowflake userId(){
        return Snowflake.of(userId);
    }

    public void userId(@NonNull Snowflake userId){
        this.userId = userId.asString();
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

    @Override
    public String toString(){
        return "MessageInfo{" +
               "channelId='" + channelId + '\'' +
               ", content='" + content + '\'' +
               ", timestamp=" + timestamp +
               "} " + super.toString();
    }
}
