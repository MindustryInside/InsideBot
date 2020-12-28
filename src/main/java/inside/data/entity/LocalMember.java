package inside.data.entity;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import inside.data.entity.base.GuildEntity;
import org.joda.time.*;
import reactor.util.annotation.*;

import javax.persistence.*;
import java.util.Calendar;

@Entity
@Table(name = "local_member")
public class LocalMember extends GuildEntity{
    private static final long serialVersionUID = -9169934990408633927L;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "effective_name", length = 32)
    private String effectiveName;

    @Column(name = "message_seq")
    private long messageSeq;

    @Column(name = "last_sent_message")
    private Calendar lastSentMessage;

    public LocalMember(){}

    public LocalMember(Member member){
        guildId(member.getGuildId());
        userId = member.getId().asString();
        effectiveName(member.getDisplayName());
    }

    @Transient
    public void addToSeq(){
        messageSeq++;
    }

    @Transient
    public boolean isActiveUser(){
        if(lastSentMessage() == null) return false;
        DateTime last = new DateTime(lastSentMessage());
        int diff = Weeks.weeksBetween(last, DateTime.now()).getWeeks();

        return diff < 3 && messageSeq() >= 75;
    }

    @NonNull
    public Snowflake userId(){
        return Snowflake.of(userId);
    }

    @NonNull
    public String effectiveName(){
        return effectiveName;
    }

    public void effectiveName(@NonNull String effectiveName){
        this.effectiveName = effectiveName;
    }

    public long messageSeq(){
        return messageSeq;
    }

    public void messageSeq(long messageSeq){
        this.messageSeq = messageSeq;
    }

    @Nullable
    public Calendar lastSentMessage(){
        return lastSentMessage;
    }

    public void lastSentMessage(@Nullable Calendar lastSentMessage){
        this.lastSentMessage = lastSentMessage;
    }

    @Override
    public String toString(){
        return "LocalMember{" +
               "userId='" + userId + '\'' +
               ", effectiveName='" + effectiveName + '\'' +
               ", messageSeq=" + messageSeq +
               ", lastSentMessage=" + lastSentMessage +
               "} " + super.toString();
    }
}
