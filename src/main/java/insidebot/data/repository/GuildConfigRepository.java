package insidebot.data.repository;

import insidebot.data.entity.GuildConfig;
import insidebot.data.repository.base.GuildRepository;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GuildConfigRepository extends JpaRepository<GuildConfig, String>{

    boolean existsByGuildId(@Param("guildId") String guildId);

    @Query("select e from GuildConfig e where e.guildId = :guildId")
    GuildConfig findByGuildId(@Param("guildId") String guildId);

    @Query("select e.prefix from GuildConfig e where e.guildId = :guildId")
    String findPrefixByGuildId(@Param("guildId") String guildId);

    @Query("select e.locale from GuildConfig e where e.guildId = :guildId")
    String findLocaleByGuildId(@Param("guildId") String guildId);

    @Query("select e.logChannelId from GuildConfig e where e.guildId = :guildId")
    String findLogChannelIdByGuildId(@Param("guildId") String guildId);

    @Query("select e.muteRoleID from GuildConfig e where e.guildId = :guildId")
    String findMuteRoleIdIdByGuildId(@Param("guildId") String guildId);

    @Query("select e.activeUserRoleID from GuildConfig e where e.guildId = :guildId")
    String findActiveUserIdByGuildId(@Param("guildId") String guildId);
}
