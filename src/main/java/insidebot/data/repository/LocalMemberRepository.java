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

    @Query("select m from LocalMember m where m.guildId = :guildId and m.id = :userId")
    LocalMember findByGuildIdAndId(@Param("guildId") String guildId, @Param("userId") String userId);

    default LocalMember findByGuildIdAndId(@NonNull Snowflake guildId, @NonNull Snowflake userId){
        return findByGuildIdAndId(guildId.asString(), userId.asString());
    }
}
