package inside.data.service.impl;

import inside.Settings;
import inside.data.entity.AdminConfig;
import inside.data.repository.AdminConfigRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminConfigService extends BaseLongObjEntityService<AdminConfig, AdminConfigRepository>{

    protected AdminConfigService(AdminConfigRepository repository, Settings settings){
        super(repository, settings);
    }

    @Override
    protected AdminConfig find0(Long id){
        return repository.findByGuildId(id);
    }
}
