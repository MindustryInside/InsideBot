package inside.data.service.impl;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.*;
import inside.Settings;
import inside.data.entity.*;
import inside.data.repository.*;
import inside.data.service.*;
import inside.util.LocaleUtil;
import org.joda.time.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.util.*;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EntityRetrieverImpl implements EntityRetriever{
    private static final Logger log = Loggers.getLogger(EntityRetriever.class);

    private final BaseEntityService<Snowflake, GuildConfig, GuildConfigRepository> guildConfigService;

    private final BaseEntityService<Snowflake, AdminConfig, AdminConfigRepository> adminConfigService;

    private final BaseEntityService<Member, LocalMember, LocalMemberRepository> memberService;

    public EntityRetrieverImpl(
            @Autowired BaseEntityService<Snowflake, GuildConfig, GuildConfigRepository> guildConfigService,
            @Autowired BaseEntityService<Snowflake, AdminConfig, AdminConfigRepository> adminConfigService,
            @Autowired BaseEntityService<Member, LocalMember, LocalMemberRepository> memberService
    ){
        this.guildConfigService = guildConfigService;
        this.adminConfigService = adminConfigService;
        this.memberService = memberService;
    }

    @Override
    public GuildConfig getGuildById(Snowflake guildId){
        return guildConfigService.find(guildId);
    }

    @Override
    public String getPrefix(Snowflake guildId){
        return getGuildById(guildId).prefix();
    }

    @Override
    public Locale getLocale(Snowflake guildId){
        return getGuildById(guildId).locale();
    }

    @Override
    public DateTimeZone getTimeZone(Snowflake guildId){
        return getGuildById(guildId).timeZone();
    }

    @Override
    public Optional<Snowflake> getLogChannelId(Snowflake guildId){
        return getGuildById(guildId).logChannelId();
    }

    @Override
    public void save(GuildConfig entity){
        guildConfigService.save(entity);
    }

    @Override
    public List<Snowflake> getAdminRoleIds(Snowflake guildId){
        return getAdminConfigById(guildId).adminRoleIDs().stream()
                .map(Snowflake::of)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public void save(AdminConfig config){
        adminConfigService.save(config);
    }

    @Override
    public AdminConfig getAdminConfigById(Snowflake guildId){
        return adminConfigService.find(guildId);
    }

    @Override
    public Optional<Snowflake> getMuteRoleId(Snowflake guildId){
        return getAdminConfigById(guildId).muteRoleID();
    }

    @Override
    public LocalMember getMember(Member member){
        return memberService.find(member);
    }

    @Override
    public void save(LocalMember member){
        memberService.save(member);
    }
}
