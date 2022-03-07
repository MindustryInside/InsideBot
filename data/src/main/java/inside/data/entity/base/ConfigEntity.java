package inside.data.entity.base;

import inside.data.annotation.Column;
import inside.data.annotation.MapperSuperclass;

@MapperSuperclass
public interface ConfigEntity extends GuildEntity {

    @Column
    boolean enabled();
}
