package inside.data.repository;

import inside.data.entity.AuditAction;
import inside.data.repository.base.GuildRepository;
import org.springframework.stereotype.Repository;

import java.util.Calendar;

@Repository
public interface AuditActionRepository extends GuildRepository<AuditAction>{

    void deleteByTimestampBefore(Calendar timestamp);
}
