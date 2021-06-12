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

    @Transient
    public void incrementMessageCount(){
        messageCount += 1;
    }

    @Transient
    public void resetIfAfter(){
        if(lastSentMessage != null &&  Weeks.weeksBetween(lastSentMessage, DateTime.now()).getWeeks() > 3){
            messageCount = 0;
        }
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

    @Override
    public String toString(){
        return "Activity{" +
                "messageCount=" + messageCount +
                ", lastSentMessage=" + lastSentMessage +
                "} " + super.toString();
    }
}
