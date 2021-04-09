package inside.data.entity;

import discord4j.common.util.Snowflake;
import inside.data.entity.base.GuildEntity;
import org.hibernate.annotations.Type;
import org.joda.time.Duration;
import reactor.util.annotation.Nullable;

import javax.persistence.*;
import java.io.Serial;
import java.util.*;

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
    private int maxWarnCount;

    @Column(name = "mute_role_id")
    private String muteRoleId;

    /* lazy initializing */
    @Type(type = "json")
    @Column(name = "admin_role_ids", columnDefinition = "json")
    private List<String> adminRoleIds;

    public Duration warnExpireDelay(){
        return warnExpireDelay;
    }

    public void warnExpireDelay(@Nullable Duration warnExpireDelay){
        this.warnExpireDelay = warnExpireDelay;
    }

    @Nullable
    public Duration muteBaseDelay(){
        return muteBaseDelay;
    }

    public void muteBaseDelay(@Nullable Duration muteBaseDelay){
        this.muteBaseDelay = muteBaseDelay;
    }

    public int maxWarnCount(){
        return maxWarnCount;
    }

    public void maxWarnCount(int maxWarnCount){
        this.maxWarnCount = maxWarnCount;
    }

    public Optional<Snowflake> muteRoleID(){
        return Optional.ofNullable(muteRoleId).map(Snowflake::of);
    }

    public void muteRoleId(Snowflake muteRoleId){
        this.muteRoleId = Objects.requireNonNull(muteRoleId, "muteRoleId").asString();
    }

    public List<String> adminRoleIDs(){
        if(adminRoleIds == null){
            adminRoleIds = new ArrayList<>();
        }
        return adminRoleIds;
    }

    public void adminRoleIDs(List<String> adminRoleIDs){
        this.adminRoleIds = Objects.requireNonNull(adminRoleIDs, "adminRoleIDs");
    }

    @Override
    public String toString(){
        return "AdminConfig{" +
                "warnExpireDelay=" + warnExpireDelay +
                ", muteBaseDelay=" + muteBaseDelay +
                ", maxWarnCount=" + maxWarnCount +
                ", muteRoleId='" + muteRoleId + '\'' +
                ", adminRoleIds=" + adminRoleIds +
                "} " + super.toString();
    }
}