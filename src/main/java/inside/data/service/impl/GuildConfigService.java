package inside.data.service.impl;

import inside.Settings;
import inside.data.entity.GuildConfig;
import inside.data.repository.GuildConfigRepository;
import org.springframework.stereotype.Service;

@Service
public class GuildConfigService extends BaseLongObjEntityService<GuildConfig, GuildConfigRepository>{

    protected GuildConfigService(GuildConfigRepository repository, Settings settings){
        super(repository, settings);
    }

    @Override
    protected GuildConfig find0(Long id){
        return repository.findByGuildId(id);
    }
}
