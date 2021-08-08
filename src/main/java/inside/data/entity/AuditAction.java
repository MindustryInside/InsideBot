package inside.data.entity;

import inside.audit.*;
import inside.data.entity.base.*;
import org.hibernate.annotations.Type;
import reactor.util.annotation.Nullable;

import javax.persistence.*;
import java.io.Serial;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "audit_action")
public class AuditAction extends GuildEntity{
    @Serial
    private static final long serialVersionUID = 165904719880729938L;

    @Column
    private Instant timestamp;

    @Column
    @Enumerated(EnumType.STRING)
    private AuditActionType type;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "id", column = @Column(name = "source_user_id", length = 21)),
            @AttributeOverride(name = "name", column = @Column(name = "source_user_name")),
            @AttributeOverride(name = "discriminator", column = @Column(name = "source_user_discriminator"))
    })
    private NamedReference user;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "id", column = @Column(name = "target_user_id", length = 21)),
            @AttributeOverride(name = "name", column = @Column(name = "target_user_name")),
            @AttributeOverride(name = "discriminator", column = @Column(name = "target_user_discriminator"))
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

    public Instant getTimestamp(){
        return timestamp;
    }

    public void setTimestamp(Instant timestamp){
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
    }

    public AuditActionType getType(){
        return type;
    }

    public void setType(AuditActionType type){
        this.type = Objects.requireNonNull(type, "type");
    }

    public NamedReference getUser(){
        return user;
    }

    public void setUser(NamedReference user){
        this.user = Objects.requireNonNull(user, "user");
    }

    @Nullable
    public NamedReference getTarget(){
        return target;
    }

    public void setTarget(@Nullable NamedReference target){
        this.target = target;
    }

    public NamedReference getChannel(){
        return channel;
    }

    public void setChannel(NamedReference channel){
        this.channel = Objects.requireNonNull(channel, "channel");
    }

    public Map<String, Object> getAttributes(){
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes){
        this.attributes = Objects.requireNonNull(attributes, "attributes");
    }

    @Transient
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(Attribute<T> key){
        if(attributes == null || attributes.isEmpty()){
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
