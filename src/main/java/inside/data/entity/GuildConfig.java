package inside.data.entity;

import discord4j.common.util.Snowflake;
import inside.data.entity.base.*;
import org.hibernate.annotations.Type;
import reactor.core.publisher.Flux;
import reactor.util.annotation.NonNull;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "guild_config")
public class GuildConfig extends GuildEntity{

    @Column
    private String locale;

    @Column
    private String prefix;

    @Column(name = "log_channel_id")
    private String logChannelId;

    @Column(name = "mute_role_id")
    private String muteRoleID;

    @Column(name = "active_user_role_id")
    private String activeUserRoleID;

    @Type(type = "jsonb")
    @Column(name = "admin_role_ids", columnDefinition = "json")
    private List<String> adminRoleIDs;

    public GuildConfig(){}

    public GuildConfig(Snowflake guildId, Locale locale, String prefix){
        guildId(guildId);
        locale(locale);
        prefix(prefix);
    }

    @NonNull
    public Locale locale(){
        return new Locale(locale);
    }

    public void locale(@NonNull Locale locale){
        locale(locale.toString());
    }

    public void locale(@NonNull String locale){
        this.locale = locale;
    }

    @NonNull
    public String prefix(){
        return prefix;
    }

    public void prefix(@NonNull String prefix){
        this.prefix = prefix;
    }

    public Snowflake logChannelId(){
        return Snowflake.of(logChannelId);
    }

    public void logChannelId(Snowflake logChannelId){
        this.logChannelId = logChannelId.asString();
    }

    public Snowflake muteRoleID(){
        return Snowflake.of(muteRoleID);
    }

    public void muteRoleID(Snowflake muteRoleID){
        this.muteRoleID = muteRoleID.asString();
    }

    public Snowflake activeUserRoleID(){
        return Snowflake.of(activeUserRoleID);
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

    public List<Snowflake> adminRoleIdsAsList(){
        return adminRoleIDs().collectList().block();
    }

    public void addAdminRole(Snowflake roleId){
        if(adminRoleIDs == null){
            adminRoleIDs = new ArrayList<>();
        }
        adminRoleIDs.add(roleId.asString());
    }

    public void adminRoleIDs(List<String> adminRoleIDs){
        this.adminRoleIDs = adminRoleIDs;
    }

    @Override
    public String toString(){
        return "GuildConfig{" +
               "locale='" + locale + '\'' +
               ", prefix='" + prefix + '\'' +
               ", logChannelId='" + logChannelId + '\'' +
               ", muteRoleID='" + muteRoleID + '\'' +
               ", activeUserRoleID='" + activeUserRoleID + '\'' +
               ", adminRoleIDs=" + adminRoleIDs +
               "} " + super.toString();
    }
}
