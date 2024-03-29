package inside.data.entity;

import discord4j.common.util.Snowflake;
import inside.data.entity.base.ConfigEntity;

import javax.persistence.*;
import java.io.Serial;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "activity_config")
public class ActivityConfig extends ConfigEntity{
    @Serial
    private static final long serialVersionUID = -3848703477201041407L;

    @Column(name = "keep_counting_duration")
    private Duration keepCountingDuration;

    @Column(name = "message_barrier")
    private int messageBarrier;

    @Column(name = "role_id")
    private String roleId;

    @Transient
    public boolean resetIfAfter(Activity activity){
        Objects.requireNonNull(activity, "activity");
        Instant last = activity.getLastSentMessage();
        if(last != null && last.isBefore(Instant.now().minus(keepCountingDuration))){
            activity.setMessageCount(0);
            return true;
        }
        return false;
    }

    @Transient
    public boolean isActive(Activity activity){
        Objects.requireNonNull(activity, "activity");
        Instant last = activity.getLastSentMessage();
        return last != null && last.isAfter(Instant.now().minus(keepCountingDuration)) &&
                activity.getMessageCount() >= messageBarrier;
    }

    public Duration getKeepCountingDuration(){
        return keepCountingDuration;
    }

    public void setKeepCountingDuration(Duration keepCountingDuration){
        this.keepCountingDuration = Objects.requireNonNull(keepCountingDuration, "keepCountingDuration");
    }

    public int getMessageBarrier(){
        return messageBarrier;
    }

    public void setMessageBarrier(int messageBarrier){
        this.messageBarrier = messageBarrier;
    }

    public Optional<Snowflake> getRoleId(){
        return Optional.ofNullable(roleId).map(Snowflake::of);
    }

    public void setRoleId(Snowflake roleId){
        this.roleId = Objects.requireNonNull(roleId, "roleId").asString();
    }

    @Override
    public String toString(){
        return "ActivityConfig{" +
                "keepCountingDuration=" + keepCountingDuration +
                ", messageBarrier=" + messageBarrier +
                ", roleId='" + roleId + '\'' +
                "} " + super.toString();
    }
}
