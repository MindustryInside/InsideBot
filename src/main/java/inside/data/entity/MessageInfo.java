package inside.data.entity;

import discord4j.common.util.Snowflake;
import inside.data.entity.base.GuildEntity;
import org.hibernate.annotations.Type;
import reactor.util.annotation.NonNull;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "message_info")
public class MessageInfo extends GuildEntity{
    private static final long serialVersionUID = -7977287922184407665L;

    @Column(name = "message_id")
    private String messageId;

    @Column(name = "user_id")
    private String userId;

    @Column(length = 2000)
    private String content;

    @Type(type = "jsonb")
    @Column(columnDefinition = "json")
    private Map<String, String> attachments = new HashMap<>();

    @Column
    private Calendar timestamp;

    @NonNull
    public Snowflake messageId(){
        return Snowflake.of(messageId);
    }

    public void messageId(@NonNull Snowflake messageId){
        this.messageId = messageId.asString();
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

    public Map<String, String> attachments(){
        return attachments;
    }

    public void attachments(Map<String, String> attachments){
        this.attachments = attachments;
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
               "messageId='" + messageId + '\'' +
               ", userId='" + userId + '\'' +
               ", content='" + content + '\'' +
               ", attachments=" + attachments +
               ", timestamp=" + timestamp +
               "} " + super.toString();
    }
}
