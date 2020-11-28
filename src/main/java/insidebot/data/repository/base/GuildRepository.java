package insidebot.data.repository.base;

import discord4j.common.util.Snowflake;
import insidebot.data.entity.base.GuildEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import reactor.util.annotation.NonNull;

import java.util.List;

@NoRepositoryBean
public interface GuildRepository<T extends GuildEntity> extends JpaRepository<T, String>{

    T findByGuildId(@NonNull String guildId);

    default T findByGuildId(@NonNull Snowflake guildId){
        return findByGuildId(guildId.asString());
    }

    List<T> findAllByGuildId(@NonNull String guildId);

    default List<T> findAllByGuildId(@NonNull Snowflake guildId){
        return findAllByGuildId(guildId.asString());
    }

    boolean existsByGuildId(@NonNull String guildId);

    default boolean existsByGuildId(@NonNull Snowflake guildId){
        return existsByGuildId(guildId.asString());
    }
}
