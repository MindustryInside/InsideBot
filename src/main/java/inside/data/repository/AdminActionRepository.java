package inside.data.repository;

import inside.data.entity.AdminAction;
import inside.data.repository.base.GuildRepository;
import inside.data.service.AdminService.AdminActionType;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminActionRepository extends GuildRepository<AdminAction>{

    List<AdminAction> findAllByType(AdminActionType type);

    List<AdminAction> findAdminActionsByTypeAndGuildIdAndTargetId(AdminActionType type, String guildId, String targetId);
}
