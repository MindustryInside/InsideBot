package inside.data.entity;

import discord4j.common.util.Snowflake;
import inside.data.entity.base.GuildEntity;

import javax.persistence.*;
import java.io.Serial;
import java.util.*;

@Entity
@Table(name = "starboard_config")
public class StarboardConfig extends GuildEntity{
    @Serial
    private static final long serialVersionUID = -6712118196617253351L;

    @Column(name = "lower_star_barrier")
    private int lowerStarBarrier;

    @Column(name = "starboard_channel_id")
    private String starboardChannelId;

    @Column
    private boolean enable;

    public int lowerStarBarrier(){
        return lowerStarBarrier;
    }

    public void lowerStarBarrier(int lowerStarBarrier){
        this.lowerStarBarrier = lowerStarBarrier;
    }

    public Optional<Snowflake> starboardChannelId(){
        return Optional.ofNullable(starboardChannelId).map(Snowflake::of);
    }

    public void starboardChannelId(Snowflake starboardChannelId){
        this.starboardChannelId = Objects.requireNonNull(starboardChannelId, "starboardChannelId").asString();
    }

    public boolean isEnable(){
        return enable;
    }

    public void setEnable(boolean enable){
        this.enable = enable;
    }

    @Override
    public String toString(){
        return "StarboardConfig{" +
                "lowerStarBarrier=" + lowerStarBarrier +
                ", starboardChannelId='" + starboardChannelId + '\'' +
                ", enable=" + enable +
                "} " + super.toString();
    }
}
