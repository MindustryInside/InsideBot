package inside.data.entity;

import discord4j.common.util.Snowflake;
import discord4j.discordjson.Id;
import inside.data.entity.base.GuildEntity;
import org.hibernate.annotations.Type;
import reactor.util.annotation.Nullable;

import javax.persistence.*;
import java.io.Serial;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "admin_config")
public class AdminConfig extends GuildEntity{
    @Serial
    private static final long serialVersionUID = 3870326130058466675L;

    @Column(name = "warn_expire_delay")
    private Duration warnExpireDelay;

    @Column(name = "mute_base_delay")
    private Duration muteBaseDelay;

    @Column(name = "max_warn_count")
    private long maxWarnCount;

    @Column(name = "threshold_action")
    @Enumerated(EnumType.STRING)
    private AdminActionType thresholdAction;

    @Column(name = "mute_role_id")
    private String muteRoleId;

    @Type(type = "json")
    @Column(name = "admin_role_ids", columnDefinition = "json")
    private Set<Id> adminRoleIds;

    @Nullable
    public Duration getWarnExpireDelay(){
        return warnExpireDelay;
    }

    public void setWarnExpireDelay(@Nullable Duration warnExpireDelay){
        this.warnExpireDelay = warnExpireDelay;
    }

    public Duration getMuteBaseDelay(){
        return muteBaseDelay;
    }

    public void setMuteBaseDelay(Duration muteBaseDelay){
        this.muteBaseDelay = muteBaseDelay;
    }

    public long getMaxWarnCount(){
        return maxWarnCount;
    }

    public void setMaxWarnCount(long maxWarnCount){
        this.maxWarnCount = maxWarnCount;
    }

    public Optional<Snowflake> getMuteRoleID(){
        return Optional.ofNullable(muteRoleId).map(Snowflake::of);
    }

    public void setMuteRoleId(Snowflake muteRoleId){
        this.muteRoleId = Objects.requireNonNull(muteRoleId, "muteRoleId").asString();
    }

    public Set<Snowflake> getAdminRoleIds(){
        if(adminRoleIds == null){
            adminRoleIds = new HashSet<>();
        }
        return adminRoleIds.stream()
                .map(Snowflake::of)
                .collect(Collectors.toSet());
    }

    public void setAdminRoleIds(Set<Snowflake> adminRoleIDs){
        Objects.requireNonNull(adminRoleIDs, "adminRoleIDs");
        adminRoleIds = adminRoleIDs.stream()
                .map(id -> Id.of(id.asLong()))
                .collect(Collectors.toSet());
    }

    public AdminActionType getThresholdAction(){
        return thresholdAction;
    }

    public void setThresholdAction(AdminActionType thresholdAction){
        this.thresholdAction = Objects.requireNonNull(thresholdAction, "thresholdAction");
    }

    @Override
    public String toString(){
        return "AdminConfig{" +
                "warnExpireDelay=" + warnExpireDelay +
                ", muteBaseDelay=" + muteBaseDelay +
                ", maxWarnCount=" + maxWarnCount +
                ", thresholdAction=" + thresholdAction +
                ", muteRoleId='" + muteRoleId + '\'' +
                ", adminRoleIds=" + adminRoleIds +
                "} " + super.toString();
    }
}
