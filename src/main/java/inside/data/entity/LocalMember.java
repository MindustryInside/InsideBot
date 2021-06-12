package inside.data.entity;

import discord4j.common.util.Snowflake;
import inside.data.entity.base.GuildEntity;
import org.joda.time.*;

import javax.persistence.*;
import java.io.Serial;
import java.util.Objects;

@Entity
@Table(name = "local_member")
public class LocalMember extends GuildEntity{
    @Serial
    private static final long serialVersionUID = -9169934990408633927L;

    @Column(name = "user_id")
    private long userId;

    @Column(name = "effective_name", length = 32)
    private String effectiveName;

    @OneToOne(cascade = CascadeType.ALL)
    private Activity activity;

    public Snowflake userId(){
        return Snowflake.of(userId);
    }

    public void userId(Snowflake userId){
        this.userId = Objects.requireNonNull(userId, "userId").asLong();
    }

    public String effectiveName(){
        return effectiveName;
    }

    public void effectiveName(String effectiveName){
        this.effectiveName = Objects.requireNonNull(effectiveName, "effectiveName");
    }

    public Activity activity(){
        return activity;
    }

    public void activity(Activity activity){
        this.activity = activity;
    }

    @Transient
    public boolean isActiveUser(){
        DateTime last = activity.lastSentMessage();
        return last != null && Weeks.weeksBetween(last, DateTime.now()).getWeeks() < 3 &&
                activity.messageCount() >= 75; // TODO: save to GuildConfig
    }

    @Override
    public String toString(){
        return "LocalMember{" +
                "userId=" + userId +
                ", effectiveName='" + effectiveName + '\'' +
                ", activity=" + activity +
                "} " + super.toString();
    }
}
