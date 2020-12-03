package insidebot.data.repository;

import discord4j.common.util.Snowflake;
import insidebot.data.entity.GuildConfig;
import insidebot.data.repository.base.GuildRepository;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GuildConfigRepository extends GuildRepository<GuildConfig>{

    boolean existsByGuildId(String guildId);

    default boolean existsByGuildId(Snowflake guildId){
        return existsByGuildId(guildId.asString());
    }

    @Query("select e.prefix from GuildConfig e where e.guildId = :#{#guildId.asString()}")
    Optional<String> findPrefixByGuildId(@Param("guildId") Snowflake guildId);

    @Query("select e.locale from GuildConfig e where e.guildId = :#{#guildId.asString()}")
    Optional<String> findLocaleByGuildId(@Param("guildId") Snowflake guildId);

    @Query("select e.logChannelId from GuildConfig e where e.guildId = :#{#guildId.asString()}")
    Optional<String> findLogChannelIdByGuildId(@Param("guildId") Snowflake guildId);

    @Query("select e.muteRoleID from GuildConfig e where e.guildId = :#{#guildId.asString()}")
    Optional<String> findMuteRoleIdIdByGuildId(@Param("guildId") Snowflake guildId);

    @Query("select e.activeUserRoleID from GuildConfig e where e.guildId = :#{#guildId.asString()}")
    Optional<String> findActiveUserIdByGuildId(@Param("guildId") Snowflake guildId);
}
