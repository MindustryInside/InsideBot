package inside.data.entity;

import discord4j.common.util.Snowflake;
import inside.data.entity.base.GuildEntity;
import org.hibernate.annotations.Type;

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
    private String muteRoleId;

    /* lazy initializing */
    @Type(type = "jsonb")
    @Column(name = "admin_role_ids", columnDefinition = "json")
    private List<String> adminRoleIds;

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

    public Optional<Snowflake> logChannelId(){
        return Optional.ofNullable(logChannelId).map(Snowflake::of);
    }

    public void logChannelId(Snowflake logChannelId){
        this.logChannelId = Objects.requireNonNull(logChannelId, "logChannelId").asString();
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
        return "GuildConfig{" +
               "prefix='" + prefix + '\'' +
               ", locale=" + locale +
               ", timeZone=" + timeZone +
               ", logChannelId='" + logChannelId + '\'' +
               ", muteRoleID='" + muteRoleId + '\'' +
               ", adminRoleIds=" + adminRoleIds +
               "} " + super.toString();
    }
}
