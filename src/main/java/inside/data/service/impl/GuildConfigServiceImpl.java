package inside.data.service.impl;

import discord4j.common.util.Snowflake;
import inside.Settings;
import inside.data.entity.GuildConfig;
import inside.data.repository.GuildConfigRepository;
import inside.data.service.*;
import inside.util.LocaleUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class GuildConfigServiceImpl extends BaseEntityService<Snowflake, GuildConfig, GuildConfigRepository>{

    protected GuildConfigServiceImpl(GuildConfigRepository repository, Settings settings){
        super(repository, settings);
    }

    @Override
    protected GuildConfig create(Snowflake id){
        GuildConfig guildConfig = new GuildConfig();
        guildConfig.guildId(id);
        guildConfig.prefix(settings.getDefaults().getPrefix());
        guildConfig.locale(LocaleUtil.getDefaultLocale());
        guildConfig.timeZone(settings.getDefaults().getTimeZone());

        return guildConfig;
    }

    @Override
    @Transactional
    protected GuildConfig get(Snowflake id){
        String guildId = id.asString();
        return repository.findByGuildId(guildId);
    }
}
