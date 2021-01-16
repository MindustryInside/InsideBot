package inside.data.service.impl;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.*;
import inside.Settings;
import inside.data.entity.*;
import inside.data.repository.*;
import inside.data.service.*;
import inside.util.LocaleUtil;
import org.joda.time.DateTimeZone;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.function.Supplier;

@Service
public class DiscordEntityRetrieveServiceImpl implements DiscordEntityRetrieveService{
    private static final Logger log = LoggerFactory.getLogger(DiscordEntityRetrieveService.class);

    private final Settings settings;

    private final GuildConfigRepository guildRepository;

    private final LocalMemberRepository memberRepository;

    private final Object $lock = new Object[0];

    public DiscordEntityRetrieveServiceImpl(@Autowired Settings settings, @Autowired GuildConfigRepository guildRepository,
                                            @Autowired LocalMemberRepository memberRepository){
        this.settings = settings;
        this.guildRepository = guildRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public GuildConfig getGuild(Guild guild){
        return getGuildById(guild.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public GuildConfig getGuildById(Snowflake guildId){
        return guildRepository.findByGuildId(guildId);
    }

    @Override
    @Transactional(readOnly = true)
    public GuildConfig getGuildById(Snowflake guildId, Supplier<GuildConfig> prov){
        return existsGuildById(guildId) ? getGuildById(guildId) : prov.get();
    }

    @Override
    @Transactional
    public GuildConfig save(GuildConfig entity){
        return guildRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsGuildById(Snowflake guildId){
        return guildRepository.existsByGuildId(guildId);
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<Snowflake> adminRolesIds(Snowflake guildId){
        return getGuildById(guildId).adminRoleIDs();
    }

    @Override
    @Transactional(readOnly = true)
    public String prefix(Snowflake guildId){
        return guildRepository.findPrefixByGuildId(guildId).orElse(settings.prefix);
    }

    @Override
    @Transactional(readOnly = true)
    public Locale locale(Snowflake guildId){
        return guildRepository.findLocaleByGuildId(guildId).orElse(LocaleUtil.getDefaultLocale());
    }

    @Override
    @Transactional(readOnly = true)
    public DateTimeZone timeZone(Snowflake guildId){
        return DateTimeZone.forTimeZone(guildRepository.findTimeZoneByGuildId(guildId).orElse(TimeZone.getTimeZone("UTC")));
    }

    @Override
    @Transactional(readOnly = true)
    public Snowflake logChannelId(Snowflake guildId){
        return guildRepository.findLogChannelIdByGuildId(guildId).map(Snowflake::of).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Snowflake muteRoleId(Snowflake guildId){
        return guildRepository.findMuteRoleIdIdByGuildId(guildId).map(Snowflake::of).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Snowflake activeUserRoleId(Snowflake guildId){
        return guildRepository.findActiveUserIdByGuildId(guildId).map(Snowflake::of).orElse(null);
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

    //

    @Override
    @Transactional(readOnly = true)
    public List<LocalMember> getAllMembers(){
        return memberRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public LocalMember getMember(Member member){
        return getMemberById(member.getGuildId(), member.getId());
    }

    @Override
    @Transactional
    public LocalMember getMember(Member member, Supplier<LocalMember> prov){
        return getMemberById(member.getGuildId(), member.getId(), prov);
    }

    @Override
    @Transactional(readOnly = true)
    public LocalMember getMemberById(Snowflake guildId, Snowflake userId){
        return memberRepository.findByGuildIdAndUserId(guildId, userId);
    }

    @Override
    @Transactional
    public LocalMember getMemberById(Snowflake guildId, Snowflake userId, Supplier<LocalMember> prov){
        LocalMember localMember = getMemberById(guildId, userId);
        if(localMember == null){
            synchronized($lock){
                localMember = getMemberById(guildId, userId);
                if(localMember == null){
                    localMember = memberRepository.saveAndFlush(prov.get());
                }
            }
        }
        return localMember;
    }

    @Override
    @Transactional
    public LocalMember save(LocalMember member){
        return memberRepository.save(member);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsMemberById(Snowflake guildId, Snowflake userId){
        return getMemberById(guildId, userId) != null;
    }

    @Override
    @Transactional
    public void deleteMember(LocalMember member){
        memberRepository.delete(member);
    }

    @Override
    @Transactional
    public void deleteMemberById(Snowflake guildId, Snowflake userId){
        LocalMember member = getMemberById(guildId, userId);
        if(member != null){
            memberRepository.delete(member);
        }else{
            log.warn("Member '{}' ({}) not found", userId.asString(), guildId.asString());
        }
    }
}
