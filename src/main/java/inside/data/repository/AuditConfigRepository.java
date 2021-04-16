package inside.data.repository;

import inside.data.entity.AuditConfig;
import inside.data.repository.base.GuildRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditConfigRepository extends GuildRepository<AuditConfig>{

}
