package inside.data.service.impl;

import inside.Settings;
import inside.data.entity.AuditConfig;
import inside.data.repository.AuditConfigRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditConfigService extends BaseLongObjEntityService<AuditConfig, AuditConfigRepository>{

    protected AuditConfigService(AuditConfigRepository repository, Settings settings){
        super(repository, settings);
    }

    @Override
    protected AuditConfig find0(Long id){
        return repository.findByGuildId(id);
    }
}
