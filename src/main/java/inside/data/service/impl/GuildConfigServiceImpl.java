package inside.data.service.impl;

import discord4j.common.util.Snowflake;
import inside.data.entity.GuildConfig;
import inside.data.repository.GuildConfigRepository;
import inside.data.service.GuildConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuildConfigServiceImpl implements GuildConfigService{

    private final GuildConfigRepository repository;

    public GuildConfigServiceImpl(@Autowired GuildConfigRepository repository){
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public GuildConfig getGuildById(Snowflake guildId){
        return repository.findByGuildId(guildId.asString());
    }

    @Override
    @Transactional
    public void save(GuildConfig entity){
        repository.save(entity);
    }
}
