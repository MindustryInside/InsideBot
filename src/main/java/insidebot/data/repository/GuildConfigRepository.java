package insidebot.data.repository;

import insidebot.data.entity.GuildConfig;
import insidebot.data.repository.base.GuildRepository;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GuildConfigRepository extends JpaRepository<GuildConfig, String>{

    @Query("select e.prefix from GuildConfig e where e.id = :guildId")
    String findPrefixByGuildId(@Param("guildId") String guildId);

    @Query("select e.locale from GuildConfig e where e.id = :guildId")
    String findLocaleByGuildId(@Param("guildId") String guildId);
}
