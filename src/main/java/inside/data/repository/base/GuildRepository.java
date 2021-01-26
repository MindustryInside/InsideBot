package inside.data.repository.base;

import discord4j.common.util.Snowflake;
import inside.data.entity.base.GuildEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

@NoRepositoryBean
public interface GuildRepository<T extends GuildEntity> extends JpaRepository<T, String>{

    List<T> findAllByGuildId(String guildId);

    default List<T> findAllByGuildId(Snowflake guildId){
        return findAllByGuildId(guildId.asString());
    }

    T findByGuildId(String guildId);

    default T findByGuildId(Snowflake guildId){
        return findByGuildId(guildId.asString());
    }

    boolean existsByGuildId(String guildId);

    default boolean existsByGuildId(Snowflake guildId){
        return existsByGuildId(guildId.asString());
    }
}
