package inside.data.service.impl;

import discord4j.common.util.Snowflake;
import inside.Settings;
import inside.data.entity.AuditConfig;
import inside.data.repository.AuditConfigRepository;
import inside.data.service.BaseEntityService;
import org.springframework.stereotype.Service;

@Service
public class AuditConfigService extends BaseEntityService<Snowflake, AuditConfig, AuditConfigRepository>{

    protected AuditConfigService(AuditConfigRepository repository, Settings settings){
        super(repository, settings);
    }

    @Override
    protected AuditConfig create(Snowflake id){
        AuditConfig auditConfig = new AuditConfig();
        auditConfig.guildId(id);
        return auditConfig;
    }

    @Override
    protected AuditConfig get(Snowflake id){
        String guildId = id.asString();
        return repository.findByGuildId(guildId);
    }
}
