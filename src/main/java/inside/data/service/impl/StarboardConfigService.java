package inside.data.service.impl;

import inside.Settings;
import inside.data.entity.StarboardConfig;
import inside.data.repository.StarboardConfigRepository;
import inside.data.service.BaseLongObjEntityService;
import org.springframework.stereotype.Service;
import reactor.util.annotation.Nullable;

@Service
public class StarboardConfigService extends BaseLongObjEntityService<StarboardConfig, StarboardConfigRepository>{

    protected StarboardConfigService(StarboardConfigRepository repository, Settings settings){
        super(repository, settings);
    }

    @Nullable
    @Override
    protected StarboardConfig find0(Long id){
        return repository.findByGuildId(id);
    }
}
