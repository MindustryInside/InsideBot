package insidebot.data.entity;

import insidebot.data.entity.base.GuildEntity;
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

    @Nullable
    @Column(name = "effective_name", length = 32)
    private String effectiveName;

    @Column
    private int warns;

    @Column(name = "message_seq")
    private long messageSeq;

    @Nullable
    @Column(name = "last_sent_message")
    private Calendar lastSentMessage;

    @Nullable
    @Column(name = "mute_end_date")
    private Calendar muteEndDate;

    @Transient
    public int addWarn(){
        return ++warns;
    }

    @Transient
    public void addToSeq(){
        messageSeq += 1;
    }

    public LocalUser user(){
        return user;
    }

    public void user(@NonNull LocalUser user){
        this.user = user;
    }

    @NonNull
    public String effectiveName(){
        return effectiveName != null ? effectiveName : user.name();
    }

    public void effectiveName(@Nullable String effectiveName){
        this.effectiveName = effectiveName;
    }

    public int warns(){
        return warns;
    }

    public void warns(int warns){
        this.warns = warns;
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

    @Nullable
    public Calendar muteEndDate(){
        return muteEndDate;
    }

    public void muteEndDate(@Nullable Calendar muteEndDate){
        this.muteEndDate = muteEndDate;
    }
}
