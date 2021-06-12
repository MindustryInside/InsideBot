package inside.data.entity;

import discord4j.common.util.Snowflake;
import inside.data.entity.base.GuildEntity;

import javax.persistence.*;
import java.io.Serial;
import java.util.*;

// directly not used
@Entity
@Table(name = "active_user_config")
public class ActiveUserConfig extends GuildEntity{
    @Serial
    private static final long serialVersionUID = -3848703477201041407L;

    @Column(name = "keep_counting_period")
    private int keepCountingPeriod;

    @Column(name = "message_barrier")
    private int messageBarrier;

    @Column(name = "role_id")
    private String roleId;

    @Column
    private boolean enable;

    public int keepCountingPeriod(){
        return keepCountingPeriod;
    }

    public void keepCountingPeriod(int keepCountingPeriod){
        this.keepCountingPeriod = keepCountingPeriod;
    }

    public int messageBarrier(){
        return messageBarrier;
    }

    public void messageBarrier(int messageBarrier){
        this.messageBarrier = messageBarrier;
    }

    public Optional<Snowflake> roleId(){
        return Optional.ofNullable(roleId).map(Snowflake::of);
    }

    public void setRoleId(Snowflake roleId){
        this.roleId = Objects.requireNonNull(roleId, "roleId").asString();
    }

    public boolean isEnable(){
        return enable;
    }

    public void setEnable(boolean enable){
        this.enable = enable;
    }

    @Override
    public String toString(){
        return "ActiveUserConfig{" +
                "keepCountingPeriod=" + keepCountingPeriod +
                ", messageBarrier=" + messageBarrier +
                ", roleId='" + roleId + '\'' +
                ", enable=" + enable +
                "} " + super.toString();
    }
}
