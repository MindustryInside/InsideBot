package insidebot.data.entity;

import insidebot.data.entity.base.GuildEntity;
import org.joda.time.*;
import reactor.util.annotation.*;

import javax.persistence.*;
import java.util.Calendar;

@Entity
@Table(name = "local_member")
public class LocalMember extends GuildEntity{
    private static final long serialVersionUID = -9169934990408633927L;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private LocalUser user;

    @Column(name = "effective_name", length = 32)
    private String effectiveName;

    @Column(name = "message_seq")
    private long messageSeq;

    @Column(name = "last_sent_message")
    private Calendar lastSentMessage;

    @Transient
    public void addToSeq(){
        messageSeq++;
    }

    @Transient
    public boolean isActiveUser(){
        if(lastSentMessage() == null) return false;
        DateTime last = new DateTime(lastSentMessage());
        int diff = Weeks.weeksBetween(last, DateTime.now()).getWeeks();

        if(diff >= 3){
            return false;
        }else{
            return messageSeq() >= 75;
        }
    }

    public LocalUser user(){
        return user;
    }

    public void user(@NonNull LocalUser user){
        this.user = user;
    }

    @NonNull
    @Transient
    public String username(){
        return user.name();
    }

    @NonNull
    public String effectiveName(){
        return effectiveName != null ? effectiveName : user.name();
    }

    public void effectiveName(@Nullable String effectiveName){
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
               "user=" + user +
               ", effectiveName='" + effectiveName + '\'' +
               ", messageSeq=" + messageSeq +
               ", lastSentMessage=" + lastSentMessage +
               "} " + super.toString();
    }
}
