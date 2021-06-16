package inside.event;

import discord4j.common.util.Snowflake;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.guild.*;
import discord4j.core.object.audit.ActionType;
import discord4j.core.object.entity.*;
import inside.audit.AuditService;
import inside.data.entity.AdminConfig;
import inside.data.service.*;
import inside.service.MessageService;
import inside.util.DiscordUtil;
import org.joda.time.DateTime;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.*;
import reactor.util.context.Context;

import java.time.*;

import static inside.audit.Attribute.REASON;
import static inside.audit.AuditActionType.*;
import static inside.util.ContextUtil.*;

@Component
public class MemberEventHandler extends ReactiveEventAdapter{
    // the number from which the filtering of audit logs is based
    protected static final long TIMEOUT_MILLIS = 3500L;

    @Autowired
    private EntityRetriever entityRetriever;

    @Autowired
    private AuditService auditService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private MessageService messageService;

    @Override
    public Publisher<?> onMemberJoin(MemberJoinEvent event){
        Member member = event.getMember();

        Snowflake guildId = member.getGuildId();

        Mono<Context> initContext = entityRetriever.getGuildConfigById(guildId)
                .switchIfEmpty(entityRetriever.createGuildConfig(guildId))
                .map(guildConfig -> Context.of(KEY_LOCALE, guildConfig.locale(),
                        KEY_TIMEZONE, guildConfig.timeZone()));

        Mono<AdminConfig> adminConfig = entityRetriever.getAdminConfigById(member.getGuildId());

        Mono<Void> warn = Mono.deferContextual(ctx -> member.getGuild().flatMap(Guild::getOwner)
                .filterWhen(ignored -> adminConfig.flatMap(config -> adminService.warnings(member).count()
                        .map(c -> c >= config.maxWarnCount())))
                .flatMap(owner -> adminService.warn(owner, member, messageService.get(ctx, "audit.member.warn.evade"))));

        Mono<?> muteEvade = Mono.deferContextual(ctx -> member.getGuild().flatMap(Guild::getOwner)
                .filterWhen(ignored -> adminService.isMuted(member))
                .flatMap(owner -> adminConfig.flatMap(config -> adminService.mute(owner, member,
                        DateTime.now().plus(config.muteBaseDelay()),
                        messageService.get(ctx, "audit.member.mute.evade"))
                        .thenReturn(owner)))
                .switchIfEmpty(warn.then(Mono.empty())));

        return initContext.flatMap(context -> auditService.log(event.getGuildId(), MEMBER_JOIN)
                .withUser(member)
                .save()
                .and(muteEvade)
                .contextWrite(context));
    }

    @Override
    public Publisher<?> onMemberLeave(MemberLeaveEvent event){
        User user = event.getUser();

        Snowflake guildId = event.getGuildId();

        Mono<Context> initContext = entityRetriever.getGuildConfigById(guildId)
                .switchIfEmpty(entityRetriever.createGuildConfig(guildId))
                .map(guildConfig -> Context.of(KEY_LOCALE, guildConfig.locale(),
                        KEY_TIMEZONE, guildConfig.timeZone()));

        Mono<Void> log = auditService.log(event.getGuildId(), MEMBER_LEAVE)
                .withUser(user)
                .save();

        Mono<Void> kick = Mono.deferContextual(ctx -> event.getGuild()
                .flatMapMany(guild -> guild.getAuditLog(spec -> spec.setActionType(ActionType.MEMBER_KICK)))
                .flatMap(part -> Flux.fromIterable(part.getEntries()))
                .filter(entry -> entry.getId().getTimestamp().isAfter(Instant.now(Clock.systemUTC()).minusMillis(TIMEOUT_MILLIS)) &&
                        entry.getTargetId().map(target -> target.equals(user.getId())).orElse(false))
                .next()
                .flatMap(entry -> Mono.justOrEmpty(entry.getResponsibleUser())
                        .flatMap(admin -> auditService.log(guildId, MEMBER_KICK)
                                .withUser(admin)
                                .withTargetUser(user)
                                .withAttribute(REASON, entry.getReason()
                                        .orElse(messageService.get(ctx, "common.not-defined")))
                                .save())
                        .thenReturn(entry))
                .switchIfEmpty(log.then(Mono.empty()))
                .then());

        return initContext.flatMap(ctx -> event.getGuild()
                .flatMapMany(guild -> guild.getAuditLog(spec -> spec.setActionType(ActionType.MEMBER_BAN_ADD)))
                .flatMap(part -> Flux.fromIterable(part.getEntries()))
                .filter(entry -> entry.getId().getTimestamp().isAfter(Instant.now(Clock.systemUTC()).minusMillis(TIMEOUT_MILLIS)) &&
                        entry.getTargetId().map(target -> target.equals(user.getId())).orElse(false))
                .next()
                .flatMap(entry -> Mono.justOrEmpty(entry.getResponsibleUser())
                        .flatMap(admin -> auditService.log(guildId, MEMBER_BAN)
                                .withUser(admin)
                                .withTargetUser(user)
                                .withAttribute(REASON, entry.getReason()
                                        .orElse(messageService.get(ctx, "common.not-defined")))
                                .save())
                        .thenReturn(entry))
                .switchIfEmpty(kick.then(Mono.empty()))
                .contextWrite(ctx));
    }

    @Override
    public Publisher<?> onMemberUpdate(MemberUpdateEvent event){
        return event.getMember()
                .filter(DiscordUtil::isNotBot)
                .flatMap(member -> entityRetriever.getAndUpdateLocalMemberById(member)
                        .switchIfEmpty(entityRetriever.createLocalMember(member))
                        .flatMap(localMember -> {
                            localMember.effectiveName(event.getCurrentNickname().orElse(member.getUsername()));
                            return entityRetriever.save(localMember);
                        }));
    }
}
