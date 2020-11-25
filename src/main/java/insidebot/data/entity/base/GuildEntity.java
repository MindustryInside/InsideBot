package insidebot.data.entity.base;

import javax.persistence.*;

@MappedSuperclass
public abstract class GuildEntity extends BaseEntity{
    private static final long serialVersionUID = 7731061101796511964L;

    @Column(name = "guild_id")
    protected String guildId;

    @Override
    public String toString(){
        return this.getClass().getSimpleName() + "{" +
               "guildId='" + guildId + '\'' +
               ", id='" + id + '\'' +
               '}';
    }
}
