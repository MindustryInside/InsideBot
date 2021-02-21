package inside.data.repository;

import inside.data.entity.AuditAction;
import inside.data.repository.base.GuildRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditActionRepository extends GuildRepository<AuditAction>{

}
