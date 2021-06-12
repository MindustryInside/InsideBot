package inside.data.entity;

import inside.data.entity.base.GuildEntity;
import org.joda.time.*;
import reactor.util.annotation.Nullable;

import javax.persistence.*;
import java.io.Serial;

// directly not used
@Entity
@Table(name = "activity")
public class Activity extends GuildEntity{
    @Serial
    private static final long serialVersionUID = -4910286185798200086L;

    @Column(name = "message_count")
    private int messageCount;

    @Column(name = "last_sent_message")
    private DateTime lastSentMessage;

    @OneToOne(cascade = CascadeType.ALL)
    private ActiveUserConfig activeUserConfig;

    @Transient
    public void incrementMessageCount(){
        messageCount += 1;
    }

    @Transient
    public void resetIfAfter(){
        if(lastSentMessage != null && Days.daysBetween(lastSentMessage, DateTime.now()).getDays() > activeUserConfig.keepCountingPeriod()){
            messageCount = 0;
        }
    }

    @Transient
    public boolean isActive(){
        DateTime last = lastSentMessage;
        return last != null && Days.daysBetween(lastSentMessage, DateTime.now()).getDays() < activeUserConfig.keepCountingPeriod() &&
                messageCount >= 75;
    }

    public int messageCount(){
        return messageCount;
    }

    public void messageCount(int messageCount){
        this.messageCount = messageCount;
    }

    @Nullable
    public DateTime lastSentMessage(){
        return lastSentMessage;
    }

    public void lastSentMessage(@Nullable DateTime lastSentMessage){
        this.lastSentMessage = lastSentMessage;
    }

    public ActiveUserConfig activeUserConfig(){
        return activeUserConfig;
    }

    public void activeUserConfig(ActiveUserConfig activeUserConfig){
        this.activeUserConfig = activeUserConfig;
    }

    @Override
    public String toString(){
        return "Activity{" +
                "messageCount=" + messageCount +
                ", lastSentMessage=" + lastSentMessage +
                ", activeUserConfig=" + activeUserConfig +
                "} " + super.toString();
    }
}
