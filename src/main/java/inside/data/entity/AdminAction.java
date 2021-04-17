package inside.data.entity;

import discord4j.common.util.Snowflake;
import inside.data.entity.base.GuildEntity;
import inside.data.service.AdminService.AdminActionType;
import org.hibernate.annotations.*;
import org.joda.time.DateTime;
import reactor.util.annotation.Nullable;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.*;
import java.io.Serial;
import java.util.*;

@Entity
@Table(name = "admin_action")
public class AdminAction extends GuildEntity{
    @Serial
    private static final long serialVersionUID = 5778834003599760075L;

    @Enumerated(EnumType.STRING)
    private AdminActionType type;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "admin_id")
    private LocalMember admin;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "target_id")
    private LocalMember target;

    @Column(length = 512)
    private String reason;

    @Column
    private DateTime timestamp;

    @Column(name = "end_timestamp")
    private DateTime endTimestamp;

    public static Builder builder(){
        return new Builder();
    }

    protected AdminAction(){}

    private AdminAction(Snowflake guildId, AdminActionType type,
                        LocalMember admin, LocalMember target,
                        @Nullable String reason, DateTime timestamp,
                        @Nullable DateTime endTimestamp){
        this.guildId = guildId.asLong();
        this.type = type;
        this.admin = admin;
        this.target = target;
        this.reason = reason;
        this.timestamp = timestamp;
        this.endTimestamp = endTimestamp;
    }

    public AdminActionType type(){
        return type;
    }

    public LocalMember admin(){
        return admin;
    }

    public LocalMember target(){
        return target;
    }

    public Optional<String> reason(){
        return Optional.ofNullable(reason);
    }

    public DateTime timestamp(){
        return timestamp;
    }

    public Optional<DateTime> endTimestamp(){
        return Optional.ofNullable(endTimestamp);
    }

    @Transient
    public boolean isEnd(){
        return endTimestamp().map(delay -> DateTime.now().isAfter(delay)).orElse(false);
    }

    @Override
    public String toString(){
        return "AdminAction{" +
               "type=" + type +
               ", admin=" + admin +
               ", target=" + target +
               ", reason='" + reason + '\'' +
               ", timestamp=" + timestamp +
               ", endTimestamp=" + endTimestamp +
               "} " + super.toString();
    }

    public static class Builder{
        private Snowflake guildId;
        private AdminActionType type;
        private LocalMember admin;
        private LocalMember target;
        @Nullable
        private String reason;
        private DateTime timestamp;
        @Nullable
        private DateTime endTimestamp;

        public Builder guildId(Snowflake guildId){
            this.guildId = Objects.requireNonNull(guildId, "guildId");
            return this;
        }

        public Builder type(AdminActionType type){
            this.type = Objects.requireNonNull(type, "type");
            return this;
        }

        public Builder admin(LocalMember admin){
            this.admin = Objects.requireNonNull(admin, "admin");
            return this;
        }

        public Builder target(LocalMember target){
            this.target = Objects.requireNonNull(target, "target");
            return this;
        }

        public Builder reason(@Nullable String reason){
            this.reason = reason;
            return this;
        }

        public Builder timestamp(DateTime timestamp){
            this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
            return this;
        }

        public Builder endTimestamp(@Nullable DateTime endTimestamp){
            this.endTimestamp = endTimestamp;
            return this;
        }

        public AdminAction build(){
            return new AdminAction(guildId, type, admin, target, reason, timestamp, endTimestamp);
        }
    }
}
