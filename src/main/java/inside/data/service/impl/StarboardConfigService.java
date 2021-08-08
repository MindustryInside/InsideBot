package inside.data.service.impl;

import inside.Settings;
import inside.data.entity.StarboardConfig;
import inside.data.repository.StarboardConfigRepository;
import inside.data.service.BaseLongObjEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.util.annotation.Nullable;

@Service
public class StarboardConfigService extends BaseLongObjEntityService<StarboardConfig, StarboardConfigRepository>{

    protected StarboardConfigService(StarboardConfigRepository repository, Settings settings){
        super(repository, settings.getCache().isStarboardConfig());
    }

    @Nullable
    @Override
    @Transactional(readOnly = true)
    protected StarboardConfig find0(long id){
        return repository.findByGuildId(id);
    }
}
