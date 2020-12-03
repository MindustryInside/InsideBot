package insidebot.data.repository;

import discord4j.common.util.Snowflake;
import insidebot.data.entity.LocalMember;
import insidebot.data.repository.base.GuildRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.util.annotation.NonNull;

@Repository
public interface LocalMemberRepository extends GuildRepository<LocalMember>{

    @Query("select m from LocalMember m where m.guildId = :#{#guildId.asString()} and m.user.userId = :#{#userId.asString()}")
    LocalMember findByGuildIdAndId(@Param("guildId") Snowflake guildId, @Param("userId") Snowflake userId);
}
