package inside.data.service.impl;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import inside.Settings;
import inside.data.entity.GuildConfig;
import inside.data.repository.GuildConfigRepository;
import inside.data.service.GuildService;
import inside.util.LocaleUtil;
import org.joda.time.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

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
    @Transactional(readOnly = true)
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
    public Flux<Snowflake> adminRolesIds(Snowflake guildId){
        return get(guildId).adminRoleIDs();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(Snowflake guildId){
        return repository.existsByGuildId(guildId);
    }

    @Override
    @Transactional(readOnly = true)
    public String prefix(Snowflake guildId){
        return repository.findPrefixByGuildId(guildId).orElse(settings.prefix);
    }

    @Override
    @Transactional(readOnly = true)
    public Locale locale(Snowflake guildId){
        return repository.findLocaleByGuildId(guildId).map(Locale::forLanguageTag).orElse(LocaleUtil.getDefaultLocale());
    }

    @Override
    @Transactional(readOnly = true)
    public DateTimeZone timeZone(Snowflake guildId){
        return DateTimeZone.forID(repository.findTimeZoneByGuildId(guildId).orElse("UTC"));
    }

    @Override
    @Transactional(readOnly = true)
    public Snowflake logChannelId(Snowflake guildId){
        return repository.findLogChannelIdByGuildId(guildId).map(Snowflake::of).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Snowflake muteRoleId(Snowflake guildId){
        return repository.findMuteRoleIdIdByGuildId(guildId).map(Snowflake::of).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Snowflake activeUserRoleId(Snowflake guildId){
        return repository.findActiveUserIdByGuildId(guildId).map(Snowflake::of).orElse(null);
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
