package inside.data.entity;

import discord4j.common.util.Snowflake;
import inside.audit.AuditActionType;
import inside.data.entity.base.GuildEntity;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serial;
import java.util.*;

@Entity
@Table(name = "audit_config")
public class AuditConfig extends GuildEntity{
    @Serial
    private static final long serialVersionUID = 31286754508621209L;

    @Column(name = "log_channel_id")
    private String logChannelId;

    @Type(type = "json")
    @Column(columnDefinition = "json")
    private Set<AuditActionType> enabled;

    @Column
    private boolean enable;

    public Optional<Snowflake> logChannelId(){
        return Optional.ofNullable(logChannelId).map(Snowflake::of);
    }

    public void logChannelId(Snowflake logChannelId){
        this.logChannelId = Objects.requireNonNull(logChannelId, "logChannelId").asString();
    }

    public Set<AuditActionType> enabled(){
        if(enabled == null){
            enabled = new HashSet<>();
        }
        return enabled;
    }

    public void enabled(Set<AuditActionType> enabled){
        this.enabled = Objects.requireNonNull(enabled, "enabled");
    }

    public boolean isEnable(){
        return enable;
    }

    public void setEnable(boolean enable){
        this.enable = enable;
    }

    @Transient
    public boolean isEnabled(AuditActionType type){
        return logChannelId != null && enabled != null && enable && !enabled.isEmpty() && enabled.contains(type);
    }

    @Override
    public String toString(){
        return "AuditConfig{" +
                "logChannelId='" + logChannelId + '\'' +
                ", enabled=" + enabled +
                ", enable=" + enable +
                "} " + super.toString();
    }
}

