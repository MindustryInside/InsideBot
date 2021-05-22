package inside.data.service.impl;

import inside.Settings;
import inside.data.entity.GuildConfig;
import inside.data.repository.GuildConfigRepository;
import inside.data.service.BaseLongObjEntityService;
import org.springframework.stereotype.Service;
import reactor.util.annotation.Nullable;

@Service
public class GuildConfigService extends BaseLongObjEntityService<GuildConfig, GuildConfigRepository>{

    protected GuildConfigService(GuildConfigRepository repository, Settings settings){
        super(repository, settings);
    }

    @Nullable
    @Override
    protected GuildConfig find0(long id){
        return repository.findByGuildId(id);
    }
}
