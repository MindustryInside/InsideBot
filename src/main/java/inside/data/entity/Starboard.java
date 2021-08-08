package inside.data.entity;

import discord4j.common.util.Snowflake;
import inside.data.entity.base.GuildEntity;

import javax.persistence.*;
import java.io.Serial;
import java.util.Objects;

@Entity
@Table(name = "starboard")
public class Starboard extends GuildEntity{
    @Serial
    private static final long serialVersionUID = -4199568551897935073L;

    @Column(name = "source_message_id")
    private long sourceMessageId;

    @Column(name = "target_message_id")
    private long targetMessageId;

    public Snowflake getSourceMessageId(){
        return Snowflake.of(sourceMessageId);
    }

    public void setSourceMessageId(Snowflake sourceMessageId){
        this.sourceMessageId = Objects.requireNonNull(sourceMessageId, "sourceMessageId").asLong();
    }

    public Snowflake getTargetMessageId(){
        return Snowflake.of(targetMessageId);
    }

    public void setTargetMessageId(Snowflake targetMessageId){
        this.targetMessageId = Objects.requireNonNull(targetMessageId, "targetMessageId").asLong();
    }

    @Override
    public String toString(){
        return "Starboard{" +
                "sourceMessageId=" + sourceMessageId +
                ", targetMessageId=" + targetMessageId +
                "} " + super.toString();
    }
}
