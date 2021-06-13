package inside.data.service.impl;

import inside.Settings;
import inside.data.entity.ActiveUserConfig;
import inside.data.repository.ActiveUserConfigRepository;
import inside.data.service.BaseLongObjEntityService;
import org.springframework.stereotype.Service;
import reactor.util.annotation.Nullable;

@Service
public class ActiveUserConfigService extends BaseLongObjEntityService<ActiveUserConfig, ActiveUserConfigRepository>{

    protected ActiveUserConfigService(ActiveUserConfigRepository repository, Settings settings){
        super(repository, settings);
    }

    @Nullable
    @Override
    protected ActiveUserConfig find0(long id){
        return repository.findByGuildId(id);
    }
}
