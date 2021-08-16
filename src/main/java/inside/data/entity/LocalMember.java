package inside.data.entity;

import discord4j.common.util.Snowflake;
import discord4j.discordjson.Id;
import inside.data.entity.base.GuildEntity;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serial;
import java.util.*;
import java.util.stream.Collectors;

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

    @Type(type = "json")
    @Column(name = "last_role_ids", columnDefinition = "json")
    private Set<Id> lastRoleIds;

    public Snowflake getUserId(){
        return Snowflake.of(userId);
    }

    public void setUserId(Snowflake userId){
        this.userId = Objects.requireNonNull(userId, "userId").asLong();
    }

    public String getEffectiveName(){
        return effectiveName;
    }

    public void setEffectiveName(String effectiveName){
        this.effectiveName = Objects.requireNonNull(effectiveName, "effectiveName");
    }

    public Activity getActivity(){
        return activity;
    }

    public void setActivity(Activity activity){
        this.activity = Objects.requireNonNull(activity, "activity");
    }

    public Set<Snowflake> getLastRoleIds(){
        if(lastRoleIds == null){
            lastRoleIds = new HashSet<>();
        }
        return lastRoleIds.stream()
                .map(Snowflake::of)
                .collect(Collectors.toSet());
    }

    public void setLastRoleIds(Set<Snowflake> lastRoleIds){
        Objects.requireNonNull(lastRoleIds, "lastRoleIds");
        this.lastRoleIds = lastRoleIds.stream()
                .map(id -> Id.of(id.asLong()))
                .collect(Collectors.toSet());
    }

    @Override
    public String toString(){
        return "LocalMember{" +
                "userId=" + userId +
                ", effectiveName='" + effectiveName + '\'' +
                ", activity=" + activity +
                ", lastRoleIds=" + lastRoleIds +
                "} " + super.toString();
    }
}
