package inside.data.entity;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import inside.data.entity.base.GuildEntity;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "welcome_message")
public class WelcomeMessage extends GuildEntity{

    @Column
    private long channelId;

    @Column(length = Message.MAX_CONTENT_LENGTH)
    private String message;

    public Snowflake getChannelId(){
        return Snowflake.of(channelId);
    }

    public void setChannelId(Snowflake channelId){
        this.channelId = Objects.requireNonNull(channelId, "channelId").asLong();
    }

    public String getMessage(){
        return message;
    }

    public void setMessage(String message){
        this.message = Objects.requireNonNull(message, "message");
    }

    @Override
    public String toString(){
        return "WelcomeMessage{" +
                "channelId=" + channelId +
                ", message='" + message + '\'' +
                "} " + super.toString();
    }
}
