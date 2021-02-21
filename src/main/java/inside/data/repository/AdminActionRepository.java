package inside.data.repository;

import inside.data.entity.AdminAction;
import inside.data.repository.base.GuildRepository;
import inside.data.service.AdminService.AdminActionType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminActionRepository extends GuildRepository<AdminAction>{

    @Query("select a from AdminAction a where a.type = :type")
    List<AdminAction> findAll(AdminActionType type);

    @Query("select a from AdminAction a where a.guildId = :#{#guildId} and a.target.userId = :#{#targetId} and a.type = :type")
    List<AdminAction> find(AdminActionType type, String guildId, String targetId);
}
