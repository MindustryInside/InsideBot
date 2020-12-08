package inside.data.repository;

import discord4j.common.util.Snowflake;
import inside.data.entity.LocalMember;
import inside.data.repository.base.GuildRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalMemberRepository extends GuildRepository<LocalMember>{

    @Query("select m from LocalMember m where m.guildId = :#{#guildId?.asString()} and m.user.userId = :#{#userId?.asString()}")
    LocalMember findByGuildIdAndUserId(@Param("guildId") Snowflake guildId, @Param("userId") Snowflake userId);
}
