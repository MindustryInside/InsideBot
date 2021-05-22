package inside.data.service.impl;

import inside.Settings;
import inside.data.entity.AuditConfig;
import inside.data.repository.AuditConfigRepository;
import inside.data.service.BaseLongObjEntityService;
import org.springframework.stereotype.Service;
import reactor.util.annotation.Nullable;

@Service
public class AuditConfigService extends BaseLongObjEntityService<AuditConfig, AuditConfigRepository>{

    protected AuditConfigService(AuditConfigRepository repository, Settings settings){
        super(repository, settings);
    }

    @Nullable
    @Override
    protected AuditConfig find0(long id){
        return repository.findByGuildId(id);
    }
}
