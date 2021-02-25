package inside.data.repository;

import inside.data.entity.AuditAction;
import inside.data.repository.base.GuildRepository;
import org.joda.time.DateTime;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;

@Repository
public interface AuditActionRepository extends GuildRepository<AuditAction>{

    @Transactional
    void deleteByTimestampBefore(DateTime timestamp);
}
