package inside.data.entity;

import discord4j.common.util.Snowflake;
import inside.data.entity.base.*;
import inside.event.audit.*;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import reactor.util.annotation.Nullable;

import javax.persistence.*;
import java.io.Serial;
import java.util.*;

@Entity
@Table(name = "audit_action")
public class AuditAction extends GuildEntity{
    @Serial
    private static final long serialVersionUID = 165904719880729938L;

    @Column
    @Type(type = "date-time")
    private DateTime timestamp;

    @Column
    @Enumerated(EnumType.STRING)
    private AuditActionType type;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "id", column = @Column(name = "source_user_id", length = 21)),
            @AttributeOverride(name = "name", column = @Column(name = "source_user_name"))
    })
    private NamedReference user;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "id", column = @Column(name = "target_user_id", length = 21)),
            @AttributeOverride(name = "name", column = @Column(name = "target_user_name"))
    })
    private NamedReference target;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "id", column = @Column(name = "channel_id", length = 21)),
            @AttributeOverride(name = "name", column = @Column(name = "channel_name"))
    })
    private NamedReference channel;

    @Type(type = "json")
    @Column(columnDefinition = "json")
    private Map<String, Object> attributes;

    public AuditAction(){}

    public AuditAction(Snowflake guildId){
        this.guildId = Objects.requireNonNull(guildId, "guildId").asString();
    }

    public DateTime timestamp(){
        return timestamp;
    }

    public void timestamp(DateTime timestamp){
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
    }

    public AuditActionType type(){
        return type;
    }

    public void type(AuditActionType type){
        this.type = Objects.requireNonNull(type, "type");
    }

    public NamedReference user(){
        return user;
    }

    public void user(NamedReference user){
        this.user = Objects.requireNonNull(user, "user");
    }

    @Nullable
    public NamedReference target(){
        return target;
    }

    public void target(@Nullable NamedReference target){
        this.target = target;
    }

    public NamedReference channel(){
        return channel;
    }

    public void channel(NamedReference channel){
        this.channel = Objects.requireNonNull(channel, "channel");
    }

    public Map<String, Object> attributes(){
        return attributes;
    }

    public void attributes(Map<String, Object> attributes){
        this.attributes = Objects.requireNonNull(attributes, "attributes");
    }

    @Transient
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(Attribute<T> key){
        if(attributes == null || attributes.isEmpty() || key == null){
            return null;
        }
        Object value = attributes.get(key.name);
        return value != null ? (T)value : null;
    }

    @Override
    public String toString(){
        return "AuditAction{" +
                "timestamp=" + timestamp +
                ", type=" + type +
                ", user=" + user +
                ", target=" + target +
                ", channel=" + channel +
                ", attributes=" + attributes +
                "} " + super.toString();
    }
}
