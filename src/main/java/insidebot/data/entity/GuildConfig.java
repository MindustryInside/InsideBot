package insidebot.data.entity;

import insidebot.data.entity.base.GuildEntity;
import reactor.util.annotation.NonNull;

import javax.persistence.*;

@Entity
@Table(name = "guild_config")
public class GuildConfig extends GuildEntity{

    @NonNull
    @Column
    private String locale;

    @NonNull
    @Column
    private String prefix;

    @NonNull
    public String locale(){
        return locale;
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
