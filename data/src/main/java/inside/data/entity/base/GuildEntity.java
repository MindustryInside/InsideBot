package inside.data.entity.base;

import inside.data.annotation.Column;
import inside.data.annotation.MapperSuperclass;

@MapperSuperclass
public interface GuildEntity extends BaseEntity {

    @Column(name = "guild_id")
    long guildId();
}
