package inside.data.entity;

import discord4j.common.util.Snowflake;
import discord4j.discordjson.json.EmojiData;
import inside.data.entity.base.ConfigEntity;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serial;
import java.util.*;

@Entity
@Table(name = "starboard_config")
public class StarboardConfig extends ConfigEntity{
    @Serial
    private static final long serialVersionUID = -6712118196617253351L;

    @Column(name = "lower_star_barrier")
    private int lowerStarBarrier;

    @Column(name = "starboard_channel_id")
    private String starboardChannelId;

    @Type(type = "json")
    @Column(columnDefinition = "json")
    private List<EmojiData> emojis;

    public int getLowerStarBarrier(){
        return lowerStarBarrier;
    }

    public void setLowerStarBarrier(int lowerStarBarrier){
        this.lowerStarBarrier = lowerStarBarrier;
    }

    public Optional<Snowflake> getStarboardChannelId(){
        return Optional.ofNullable(starboardChannelId).map(Snowflake::of);
    }

    public void setStarboardChannelId(Snowflake starboardChannelId){
        this.starboardChannelId = Objects.requireNonNull(starboardChannelId, "starboardChannelId").asString();
    }

    public List<EmojiData> getEmojis(){
        return emojis;
    }

    public void setEmojis(List<EmojiData> emojis){
        this.emojis = Objects.requireNonNull(emojis, "emojis");
    }

    @Override
    public String toString(){
        return "StarboardConfig{" +
                "lowerStarBarrier=" + lowerStarBarrier +
                ", starboardChannelId='" + starboardChannelId + '\'' +
                ", emojis=" + emojis +
                "} " + super.toString();
    }
}
