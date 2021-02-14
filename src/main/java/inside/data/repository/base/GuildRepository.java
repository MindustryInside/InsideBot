package inside.data.repository.base;

import discord4j.common.util.Snowflake;
import inside.data.entity.base.GuildEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface GuildRepository<T extends GuildEntity> extends JpaRepository<T, String>{

    T findByGuildId(String guildId);

    default T findByGuildId(Snowflake guildId){
        return findByGuildId(guildId.asString());
    }

    boolean existsByGuildId(String guildId);

    default boolean existsByGuildId(Snowflake guildId){
        return existsByGuildId(guildId.asString());
    }
}
