package insidebot.data.entity;

import discord4j.common.util.Snowflake;
import insidebot.data.entity.base.*;
import reactor.util.annotation.NonNull;

import javax.persistence.*;
import java.util.Locale;

@Entity
@Table(name = "guild_config")
public class GuildConfig extends BaseEntity{

    @NonNull
    @Column
    private String locale;

    @NonNull
    @Column
    private String prefix;

    public GuildConfig(){}

    public GuildConfig(@NonNull Snowflake guildId, @NonNull Locale locale, @NonNull String prefix){
        id(guildId);
        locale(locale);
        this.prefix = prefix;
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
}
