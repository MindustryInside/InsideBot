package insidebot.data.entity;

import discord4j.common.util.Snowflake;
import insidebot.data.entity.base.GuildEntity;
import reactor.util.annotation.NonNull;

import javax.persistence.*;
import java.util.Locale;

@Entity
@Table(name = "guild_config")
public class GuildConfig extends GuildEntity{

    @NonNull
    @Column
    private String locale;

    @NonNull
    @Column
    private String prefix;

    public GuildConfig(){}

    public GuildConfig(@NonNull Snowflake guildId, @NonNull Locale locale, @NonNull String prefix){
        guildId(guildId);
        locale(locale);
        this.prefix = prefix;
    }

    @NonNull
    public String locale(){
        return locale;
    }

    public void locale(@NonNull Locale locale){
        this.locale = locale.toString();
    }

    @NonNull
    public String prefix(){
        return prefix;
    }

    public void prefix(@NonNull String prefix){
        this.prefix = prefix;
    }
}
