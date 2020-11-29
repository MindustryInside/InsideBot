package insidebot.data.entity;

import discord4j.common.util.Snowflake;
import insidebot.data.entity.base.*;
import reactor.util.annotation.NonNull;

import javax.persistence.*;
import java.util.Locale;

@Entity
@Table(name = "guild_config")
public class GuildConfig extends BaseEntity{

    @Column
    private String locale;

    @NonNull
    @Column
    private String prefix;

    @Column(name = "log_channel_id")
    private String logChannelId;

    @Column(name = "mute_role_id")
    private String muteRoleID;

    @Column(name = "active_user_role_id")
    private String activeUserRoleID;

    public GuildConfig(){}

    public GuildConfig(@NonNull Snowflake guildId, @NonNull Locale locale, @NonNull String prefix){
        id(guildId);
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
}
