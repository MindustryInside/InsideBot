package inside.data.entity.base;

import discord4j.common.util.Snowflake;

import javax.persistence.*;
import java.io.Serial;
import java.util.Objects;

@MappedSuperclass
public abstract class GuildEntity extends BaseEntity{
    @Serial
    private static final long serialVersionUID = 7731061101796511964L;

    @Column(name = "guild_id")
    protected long guildId;

    @Transient
    public Snowflake getGuildId(){
        return Snowflake.of(guildId);
    }

    public void setGuildId(Snowflake guildId){
        this.guildId = Objects.requireNonNull(guildId, "guildId").asLong();
    }

    @Override
    public boolean equals(Object o){
        if(this == o){
            return true;
        }
        if(!(o instanceof GuildEntity that)){
            return false;
        }
        if(!super.equals(o)){
            return false;
        }
        return guildId == that.guildId;
    }

    @Override
    public int hashCode(){
        return Objects.hash(super.hashCode(), guildId);
    }

    @Override
    public String toString(){
        return "GuildEntity{guildId=" + guildId + "}";
    }
}
