package insidebot.data.repository.base;

import discord4j.common.util.Snowflake;
import insidebot.data.entity.base.GuildEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

@NoRepositoryBean
public interface GuildRepository<T extends GuildEntity> extends JpaRepository<T, String>{

    @Query(value = "select g from #{#entityName} g where g.guildId = :#{#guildId?.asString()}", nativeQuery = true)
    List<T> findAllByGuildId(Snowflake guildId);

    default T findByGuildId(Snowflake guildId){
        return findAllByGuildId(guildId).get(0);
    }

    boolean existsByGuildId(String guildId);

    default boolean existsByGuildId(Snowflake guildId){
        return existsByGuildId(guildId.asString());
    }
}
