package insidebot.data.entity;

import discord4j.common.util.Snowflake;
import insidebot.data.entity.base.GuildEntity;
import insidebot.data.service.AdminService.AdminActionType;
import org.hibernate.annotations.*;
import org.joda.time.DateTime;
import reactor.util.annotation.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "admin_action")
public class AdminAction extends GuildEntity{
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

    @Column(length = 1000)
    private String reason;

    @Column
    private Calendar timestamp;

    @Column(name = "end_timestamp")
    private Calendar endTimestamp;

    public AdminAction(){}

    public AdminAction(Snowflake guildId){
        guildId(guildId);
    }

    @NonNull
    public AdminActionType type(){
        return type;
    }

    public AdminAction type(@NonNull AdminActionType type){
        this.type = type;
        return this;
    }

    @NonNull
    public LocalMember admin(){
        return admin;
    }

    public AdminAction admin(@NonNull LocalMember admin){
        this.admin = admin;
        return this;
    }

    @NonNull
    public LocalMember target(){
        return target;
    }

    public AdminAction target(@NonNull LocalMember target){
        this.target = target;
        return this;
    }

    public Optional<String> reason(){
        return Optional.ofNullable(reason);
    }

    public AdminAction reason(@Nullable String reason){
        this.reason = reason;
        return this;
    }

    @NonNull
    public Calendar timestamp(){
        return timestamp;
    }

    public AdminAction timestamp(@NonNull Calendar timestamp){
        this.timestamp = timestamp;
        return this;
    }

    @Nullable
    public Calendar end(){
        return endTimestamp;
    }

    public AdminAction end(@Nullable Calendar end){
        this.endTimestamp = end;
        return this;
    }

    @Transient
    public boolean isEnd(){
        return end() != null && DateTime.now().isAfter(new DateTime(end()));
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
}
