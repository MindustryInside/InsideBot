package inside.data.service.impl;

import discord4j.common.util.Snowflake;
import inside.Settings;
import inside.data.entity.AdminConfig;
import inside.data.repository.AdminConfigRepository;
import inside.data.service.BaseEntityService;
import org.joda.time.Duration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.util.annotation.Nullable;

@Service
public class AdminConfigServiceImpl extends BaseEntityService<Snowflake, AdminConfig, AdminConfigRepository>{

    protected AdminConfigServiceImpl(AdminConfigRepository repository, Settings settings){
        super(repository, settings);
    }

    @Override
    protected AdminConfig create(Snowflake id){
        AdminConfig adminConfig = new AdminConfig();
        adminConfig.guildId(id);
        adminConfig.maxWarnCount(3);
        adminConfig.muteBaseDelay(Duration.millis(settings.getDefaults().getMuteEvade().toMillis())); // TODO: use joda or java.time?
        adminConfig.warnExpireDelay(Duration.millis(settings.getDefaults().getWarnExpire().toMillis()));

        return adminConfig;
    }

    @Nullable
    @Override
    @Transactional
    protected AdminConfig get(Snowflake id){
        String guildId = id.asString();
        return repository.findByGuildId(guildId);
    }
}
