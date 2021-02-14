package inside.data.entity;

import discord4j.common.util.Snowflake;
import inside.data.entity.base.GuildEntity;
import org.hibernate.annotations.Type;
import reactor.core.publisher.Flux;

import javax.persistence.*;
import java.io.Serial;
import java.util.*;

@Entity
@Table(name = "guild_config")
public class GuildConfig extends GuildEntity{
    @Serial
    private static final long serialVersionUID = 2454633035779855973L;

    @Column
    private String prefix;

    @Column
    private Locale locale;

    @Column(name = "time_zone")
    private TimeZone timeZone;

    @Column(name = "log_channel_id")
    private String logChannelId;

    @Column(name = "mute_role_id")
    private String muteRoleID;

    @Column(name = "active_user_role_id")
    private String activeUserRoleID;

    /* lazy initializing */
    @Type(type = "jsonb")
    @Column(name = "admin_role_ids", columnDefinition = "json")
    private List<String> adminRoleIDs;

    public GuildConfig(){}

    public GuildConfig(Snowflake guildId){
        this.guildId = Objects.requireNonNull(guildId, "guildId").asString();
    }

    public String prefix(){
        return prefix;
    }

    public void prefix(String prefix){
        this.prefix = Objects.requireNonNull(prefix, "prefix");
    }

    public Locale locale(){
        return locale;
    }

    public void locale(Locale locale){
        this.locale = Objects.requireNonNull(locale, "locale");
    }

    public TimeZone timeZone(){
        return timeZone;
    }

    public void timeZone(TimeZone timeZone){
        this.timeZone = Objects.requireNonNull(timeZone, "timeZone");
    }

    public Snowflake logChannelId(){
        return logChannelId != null ? Snowflake.of(logChannelId) : null;
    }

    public void logChannelId(Snowflake logChannelId){
        this.logChannelId = logChannelId.asString();
    }

    public Snowflake muteRoleID(){
        return muteRoleID != null ? Snowflake.of(muteRoleID) : null;
    }

    public void muteRoleID(Snowflake muteRoleID){
        this.muteRoleID = muteRoleID.asString();
    }

    public Snowflake activeUserRoleID(){
        return activeUserRoleID != null ? Snowflake.of(activeUserRoleID) : null;
    }

    public void activeUserRoleID(Snowflake activeUserRoleID){
        this.activeUserRoleID = activeUserRoleID.asString();
    }

    public Flux<Snowflake> adminRoleIDs(){
        if(adminRoleIDs == null){
            adminRoleIDs = new ArrayList<>();
        }
        return Flux.fromIterable(adminRoleIDs).map(Snowflake::of);
    }

    public void addAdminRole(Snowflake roleId){
        Objects.requireNonNull(roleId, "roleId");
        if(adminRoleIDs == null){
            adminRoleIDs = new ArrayList<>();
        }
        adminRoleIDs.add(roleId.asString());
    }

    public void adminRoleIDs(List<String> adminRoleIDs){
        this.adminRoleIDs = Objects.requireNonNull(adminRoleIDs, "adminRoleIDs");
    }

    @Override
    public String toString(){
        return "GuildConfig{" +
               "prefix='" + prefix + '\'' +
               ", locale='" + locale + '\'' +
               ", timeZone='" + timeZone + '\'' +
               ", logChannelId='" + logChannelId + '\'' +
               ", muteRoleID='" + muteRoleID + '\'' +
               ", activeUserRoleID='" + activeUserRoleID + '\'' +
               ", adminRoleIDs=" + adminRoleIDs +
               "} " + super.toString();
    }
}
