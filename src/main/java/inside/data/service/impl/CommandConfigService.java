package inside.data.service.impl;

import inside.Settings;
import inside.data.entity.CommandConfig;
import inside.data.repository.CommandConfigRepository;
import inside.data.service.BaseEntityService;
import org.springframework.stereotype.Service;
import reactor.util.annotation.Nullable;
import reactor.util.function.Tuple2;

@Service
public class CommandConfigService extends BaseEntityService<Tuple2<Long, String>, CommandConfig, CommandConfigRepository>{

    protected CommandConfigService(CommandConfigRepository repository, Settings settings){
        super(repository, settings.getCache().isCommandConfig());
    }

    @Nullable
    @Override
    protected CommandConfig find0(Tuple2<Long, String> id){
        long guildId = id.getT1();
        String name = id.getT2();
        return repository.findByAlias(guildId, name);
    }
}
