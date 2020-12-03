package insidebot.data.repository.base;

import discord4j.common.util.Snowflake;
import insidebot.data.entity.base.GuildEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.util.List;

@NoRepositoryBean
public interface GuildRepository<T extends GuildEntity> extends JpaRepository<T, String>{

    @Query("select g from #{#entityName} g where g.guildId = :#{#guildId.asString()}")
    List<T> findAllByGuildId(@Param("guildId") Snowflake guildId);

    default T findByGuildId(Snowflake guildId){
        return findAllByGuildId(guildId).get(0);
    }

    boolean existsByGuildId(String guildId);
}
