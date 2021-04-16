package inside.data.service.impl;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import inside.data.entity.*;
import inside.data.service.EntityRetriever;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.util.*;

import java.util.*;
import java.util.stream.Collectors;

/*
 * All entity services are delay
 * generators as they block threads!
 * This is terrible, but r2dbc technology is too raw!
 *
 * We also have action services,
 * shells that wrap synchronous
 * code and work with asynchronous parts of code
 */
@Service
public class EntityRetrieverImpl implements EntityRetriever{
    private static final Logger log = Loggers.getLogger(EntityRetriever.class);

    private final GuildConfigService guildConfigService;

    private final AdminConfigService adminConfigService;

    private final MemberService memberService;

    private final AuditConfigService auditConfigService;

    public EntityRetrieverImpl(
            @Autowired GuildConfigService guildConfigService,
            @Autowired AdminConfigService adminConfigService,
            @Autowired MemberService memberService,
            @Autowired AuditConfigService auditConfigService
    ){
        this.guildConfigService = guildConfigService;
        this.adminConfigService = adminConfigService;
        this.memberService = memberService;
        this.auditConfigService = auditConfigService;
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
    public void save(GuildConfig entity){
        guildConfigService.save(entity);
    }

    @Override
    public AuditConfig getAuditConfigById(Snowflake guildId){
        return auditConfigService.find(guildId);
    }

    @Override
    public Optional<Snowflake> getLogChannelId(Snowflake guildId){
        return getAuditConfigById(guildId).logChannelId();
    }

    @Override
    public void save(AuditConfig auditConfig){
        auditConfigService.save(auditConfig);
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
