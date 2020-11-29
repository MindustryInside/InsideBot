package insidebot.data.entity.base;

import discord4j.common.util.Snowflake;
import reactor.util.annotation.NonNull;

import javax.persistence.*;
import java.util.Objects;

@MappedSuperclass
public abstract class GuildEntity extends BaseEntity{
    private static final long serialVersionUID = 7731061101796511964L;

    @Column(name = "guild_id")
    protected String guildId;

    @NonNull
    @Transient
    public Snowflake guildId(){
        return Snowflake.of(guildId);
    }

    @NonNull
    public String getGuildId(){
        return guildId;
    }

    public void guildId(@NonNull Snowflake guildId){
        this.guildId = guildId.asString();
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        if(!super.equals(o)) return false;
        GuildEntity that = (GuildEntity)o;
        return Objects.equals(guildId, that.guildId);
    }

    @Override
    public int hashCode(){
        return Objects.hash(super.hashCode(), guildId);
    }

    @Override
    public String toString(){
        return this.getClass().getSimpleName() + "{" +
               "guildId='" + guildId + '\'' +
               ", id='" + id + '\'' +
               '}';
    }
}
