package insidebot.data.service.impl;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import insidebot.Settings;
import insidebot.data.entity.GuildConfig;
import insidebot.data.repository.GuildConfigRepository;
import insidebot.data.service.GuildService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Service
public class GuildServiceImpl implements GuildService{
    @Autowired
    private Settings settings;

    @Autowired
    private GuildConfigRepository repository;

    @Override
    @Transactional(readOnly = true)
    public GuildConfig get(Guild guild){
        return get(guild.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public GuildConfig get(Snowflake guildId){
        return repository.findById(guildId.asString()).orElse(null);
    }

    @Override
    @Transactional
    public GuildConfig getOr(Snowflake guildId, Supplier<GuildConfig> prov){
        return exists(guildId) ? get(guildId) : prov.get();
    }

    @Override
    @Transactional
    public GuildConfig save(GuildConfig entity){
        return repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(Snowflake guildId){
        return repository.existsById(guildId.asString());
    }

    @Override
    @Transactional(readOnly = true)
    public String prefix(Snowflake guildId){
        String prefix = repository.findPrefixByGuildId(guildId.asString());
        return prefix != null ? prefix : settings.prefix;
    }

    @Override
    @Transactional(readOnly = true)
    public String locale(Snowflake guildId){
        return repository.findLocaleByGuildId(guildId.asString());
    }

    @Override
    @Transactional(readOnly = true)
    public Snowflake logChannelId(Snowflake guildId){
        String roleId = repository.findLogChannelIdByGuildId(guildId.asString());
        return roleId != null ? Snowflake.of(roleId) : null;
    }

    @Override
    @Transactional(readOnly = true)
    public Snowflake muteRoleId(Snowflake guildId){
        String roleId = repository.findMuteRoleIdIdByGuildId(guildId.asString());
        return roleId != null ? Snowflake.of(roleId) : null;
    }

    @Override
    @Transactional(readOnly = true)
    public Snowflake activeUserRoleId(Snowflake guildId){
        String roleId = repository.findActiveUserIdByGuildId(guildId.asString());
        return roleId != null ? Snowflake.of(roleId) : null;
    }

    @Override
    public boolean auditDisabled(Snowflake guildId){
        return logChannelId(guildId) == null;
    }

    @Override
    public boolean muteDisabled(Snowflake guildId){
        return muteRoleId(guildId) == null;
    }

    @Override
    public boolean activeUserDisabled(Snowflake guildId){
        return activeUserRoleId(guildId) == null;
    }
}
