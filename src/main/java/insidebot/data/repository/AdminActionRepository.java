package insidebot.data.repository;

import discord4j.common.util.Snowflake;
import insidebot.data.entity.AdminAction;
import insidebot.data.repository.base.GuildRepository;
import insidebot.data.service.AdminService.AdminActionType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminActionRepository extends GuildRepository<AdminAction>{
    @Query("select a from AdminAction a where a.guildId = :#{#guildId?.asString()} and a.target.user.userId = :#{#targetId?.asString()}")
    List<AdminAction> findAdminActionsByTargetId(@Param("guildId") Snowflake guildId, @Param("targetId") Snowflake targetId);

    @Query("select a from AdminAction a where a.guildId = :#{#guildId?.asString()} and a.target.user.userId = :#{#targetId?.asString()} and a.type = :type")
    List<AdminAction> findAdminActionsByTypeAndTargetId(@Param("type") AdminActionType type, @Param("guildId") Snowflake guildId, @Param("targetId") Snowflake targetId);
}
