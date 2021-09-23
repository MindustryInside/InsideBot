package inside.data.entity;

import discord4j.common.util.Snowflake;
import inside.data.entity.base.GuildEntity;

import javax.persistence.*;
import java.io.Serial;
import java.util.Objects;

@Entity
@Table(name = "poll_answer")
public class PollAnswer extends GuildEntity{
    @Serial
    private static final long serialVersionUID = -6057604094125458442L;

    @Column
    private int option;

    @Column(name = "user_id")
    private long userId;

    public int getOption(){
        return option;
    }

    public void setOption(int option){
        this.option = option;
    }

    public Snowflake getUserId(){
        return Snowflake.of(userId);
    }

    public void setUserId(Snowflake userId){
        this.userId = Objects.requireNonNull(userId, "userId").asLong();
    }

    @Override
    public String toString(){
        return "PollAnswer{" +
                "option=" + option +
                ", userId=" + userId +
                "} " + super.toString();
    }
}
