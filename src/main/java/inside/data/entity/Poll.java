package inside.data.entity;

import discord4j.common.util.Snowflake;
import inside.data.entity.base.GuildEntity;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serial;
import java.util.*;

@Entity
@Table(name = "poll")
public class Poll extends GuildEntity{
    @Serial
    private static final long serialVersionUID = 5152518412598516739L;

    @Column(name = "message_id")
    private long messageId;

    @Type(type = "json")
    @Column(columnDefinition = "json")
    private List<String> options;

    @JoinColumn(name = "pool_id")
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PollAnswer> answered = new ArrayList<>();

    public Snowflake getMessageId(){
        return Snowflake.of(messageId);
    }

    public void setMessageId(Snowflake messageId){
        this.messageId = Objects.requireNonNull(messageId, "messageId").asLong();
    }

    public List<String> getOptions(){
        return options;
    }

    public void setOptions(List<String> options){
        this.options = Objects.requireNonNull(options, "options");
    }

    public List<PollAnswer> getAnswered(){
        return answered;
    }

    public void setAnswered(List<PollAnswer> answered){
        this.answered = Objects.requireNonNull(answered, "answered");
    }

    @Override
    public String toString(){
        return "Poll{" +
                "messageId=" + messageId +
                ", options=" + options +
                ", answered=" + answered +
                "} " + super.toString();
    }
}
