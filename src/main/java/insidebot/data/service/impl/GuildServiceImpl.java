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

import java.util.Locale;
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
        return repository.findByGuildId(guildId);
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
        return repository.existsByGuildId(guildId);
    }

    @Override
    @Transactional(readOnly = true)
    public String prefix(Snowflake guildId){
        String prefix = repository.findPrefixByGuildId(guildId.asString());
        return prefix != null ? prefix : settings.prefix;
    }

    @Override
    @Transactional(readOnly = true)
    public Locale locale(Snowflake guildId){
        String locale = repository.findLocaleByGuildId(guildId.asString());
        return locale != null ? new Locale(locale) : settings.locale;
    }
}
