package inside.data.service.impl;

import inside.Settings;
import inside.data.entity.AdminConfig;
import inside.data.repository.AdminConfigRepository;
import inside.data.service.BaseLongObjEntityService;
import org.springframework.stereotype.Service;
import reactor.util.annotation.Nullable;

@Service
public class AdminConfigService extends BaseLongObjEntityService<AdminConfig, AdminConfigRepository>{

    protected AdminConfigService(AdminConfigRepository repository, Settings settings){
        super(repository, settings);
    }

    @Nullable
    @Override
    protected AdminConfig find0(long id){
        return repository.findByGuildId(id);
    }
}
