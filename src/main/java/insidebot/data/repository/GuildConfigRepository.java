package insidebot.data.repository;

import insidebot.data.entity.GuildConfig;
import insidebot.data.repository.base.GuildRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GuildConfigRepository extends GuildRepository<GuildConfig>{

    @Query("select e.prefix from GuildConfig e where e.guildId = :guildId")
    String findPrefixByGuildId(@Param("guildId") String guildId);

    @Query("select e.locale from GuildConfig e where e.guildId = :guildId")
    String findLocaleByGuildId(@Param("guildId") String guildId);
}
