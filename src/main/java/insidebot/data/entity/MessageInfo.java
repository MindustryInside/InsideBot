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

    @NonNull
    @Column(length = 2000)
    private String content;

    @NonNull
    @Column
    private Calendar timestamp;

    @NonNull
    @ManyToOne
    @JoinColumn(name = "member_id")
    private LocalMember member;

    @NonNull
    @Transient
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
    public LocalMember member(){
        return member;
    }

    public void member(@NonNull LocalMember member){
        this.member = member;
    }

    @Override
    public String toString(){
        return "MessageInfo{" +
               "channelId='" + channelId + '\'' +
               ", content='" + content + '\'' +
               ", timestamp=" + timestamp +
               ", member=" + member +
               ", guildId='" + guildId + '\'' +
               ", id='" + id + '\'' +
               '}';
    }
}
