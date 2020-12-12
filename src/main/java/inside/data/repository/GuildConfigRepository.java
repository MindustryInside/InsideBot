package inside.data.repository;

import discord4j.common.util.Snowflake;
import inside.data.entity.GuildConfig;
import inside.data.repository.base.GuildRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GuildConfigRepository extends GuildRepository<GuildConfig>{

    @Query("select e.prefix from GuildConfig e where e.guildId = :#{#guildId?.asString()}")
    Optional<String> findPrefixByGuildId(Snowflake guildId);

    @Query("select e.locale from GuildConfig e where e.guildId = :#{#guildId?.asString()}")
    Optional<String> findLocaleByGuildId(Snowflake guildId);

    @Query("select e.timeZone from GuildConfig e where e.guildId = :#{#guildId?.asString()}")
    Optional<String> findTimeZoneByGuildId(Snowflake guildId);

    @Query("select e.logChannelId from GuildConfig e where e.guildId = :#{#guildId?.asString()}")
    Optional<String> findLogChannelIdByGuildId(Snowflake guildId);

    @Query("select e.muteRoleID from GuildConfig e where e.guildId = :#{#guildId?.asString()}")
    Optional<String> findMuteRoleIdIdByGuildId(Snowflake guildId);

    @Query("select e.activeUserRoleID from GuildConfig e where e.guildId = :#{#guildId?.asString()}")
    Optional<String> findActiveUserIdByGuildId(Snowflake guildId);
}
