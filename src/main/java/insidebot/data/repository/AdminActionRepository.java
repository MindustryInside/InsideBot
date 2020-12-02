package insidebot.data.repository;

import insidebot.data.entity.AdminAction;
import insidebot.data.repository.base.GuildRepository;
import insidebot.data.service.AdminService.AdminActionType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminActionRepository extends GuildRepository<AdminAction>{
    @Query("select a from AdminAction a where a.guildId = :guildId and a.target.id = :targetId")
    List<AdminAction> findAdminActionsByTargetId(@Param("guildId") String guildId, @Param("targetId") String targetId);

    @Query("select a from AdminAction a where a.guildId = :guildId and a.target.user.userId = :targetId and a.type = :type")
    List<AdminAction> findAdminActionsByTypeAndTargetId(@Param("type") AdminActionType type, @Param("guildId") String guildId, @Param("targetId") String targetId);
}
