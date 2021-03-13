package inside.data.service.impl;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.*;
import inside.Settings;
import inside.data.entity.*;
import inside.data.service.*;
import inside.util.LocaleUtil;
import org.joda.time.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.util.*;

import java.util.*;

@Service
public class EntityRetrieverImpl implements EntityRetriever{
    private static final Logger log = Loggers.getLogger(EntityRetriever.class);

    private final Settings settings;

    private final GuildConfigService guildConfigService;

    private final MemberService memberService;

    private final Object $memberLock = new Object[0];

    private final Object $guildLock = new Object[0];

    public EntityRetrieverImpl(@Autowired Settings settings,
                               @Autowired GuildConfigService guildConfigService,
                               @Autowired MemberService memberService){
        this.settings = settings;
        this.guildConfigService = guildConfigService;
        this.memberService = memberService;
    }

    @Override
    public GuildConfig getGuildById(Snowflake guildId){
        Objects.requireNonNull(guildId, "guildId");

        GuildConfig guildConfig = guildConfigService.getGuildById(guildId);
        if(guildConfig == null){
            synchronized($guildLock){
                guildConfig = guildConfigService.getGuildById(guildId);
                if(guildConfig == null){
                    guildConfig = new GuildConfig();
                    guildConfig.guildId(guildId);
                    guildConfig.prefix(settings.prefix);
                    guildConfig.locale(LocaleUtil.getDefaultLocale());
                    guildConfig.timeZone(DateTimeZone.forID(settings.timeZone));
                    save(guildConfig);
                }
            }
        }
        return guildConfig;
    }

    @Override
    public void save(GuildConfig entity){
        guildConfigService.save(entity);
    }

    @Override
    public Flux<Snowflake> adminRolesIds(Snowflake guildId){
        return Flux.fromIterable(getGuildById(guildId).adminRoleIDs()).map(Snowflake::of);
    }

    @Override
    public String prefix(Snowflake guildId){
        return getGuildById(guildId).prefix();
    }

    @Override
    public Locale locale(Snowflake guildId){
        return getGuildById(guildId).locale();
    }

    @Override
    public DateTimeZone timeZone(Snowflake guildId){
        return getGuildById(guildId).timeZone();
    }

    @Override
    public Optional<Snowflake> logChannelId(Snowflake guildId){
        return getGuildById(guildId).logChannelId();
    }

    @Override
    public Optional<Snowflake> muteRoleId(Snowflake guildId){
        return getGuildById(guildId).muteRoleID();
    }

    @Override
    public LocalMember getMember(Member member){
        Objects.requireNonNull(member, "member");

        Snowflake guildId = member.getGuildId();
        Snowflake userId = member.getId();
        LocalMember localMember = memberService.get(guildId, userId);
        if(localMember == null){
            synchronized($memberLock){
                localMember = memberService.get(guildId, userId);
                if(localMember == null){
                    localMember = new LocalMember();
                    localMember.userId(userId);
                    localMember.guildId(guildId);
                    localMember.effectiveName(member.getDisplayName());
                    save(localMember);
                }
            }
        }
        return localMember;
    }

    @Override
    public void save(LocalMember member){
        memberService.save(member);
    }
}
