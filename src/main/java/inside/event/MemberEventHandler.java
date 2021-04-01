package inside.event;

import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.guild.*;
import discord4j.core.object.audit.ActionType;
import discord4j.core.object.entity.*;
import inside.data.entity.*;
import inside.data.service.*;
import inside.event.audit.AuditService;
import inside.service.MessageService;
import inside.util.DiscordUtil;
import org.joda.time.DateTime;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.time.Instant;

import static inside.event.audit.Attribute.REASON;
import static inside.event.audit.AuditActionType.*;
import static inside.util.ContextUtil.*;

@Component
public class MemberEventHandler extends ReactiveEventAdapter{
    protected static final long TIMEOUT_MILLIS = 2500L;

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
        if(DiscordUtil.isBot(member)){ // TODO: remove bot check
            return Mono.empty();
        }

        Context context = Context.of(KEY_LOCALE, entityRetriever.getLocale(event.getGuildId()),
                KEY_TIMEZONE, entityRetriever.getTimeZone(event.getGuildId()));

        AdminConfig config = entityRetriever.getAdminConfigById(member.getGuildId());

        Mono<Void> warn = member.getGuild().flatMap(Guild::getOwner)
                .filterWhen(ignored -> adminService.warnings(member).count().map(c -> c >= config.maxWarnCount()))
                .flatMap(owner -> adminService.warn(owner, member, messageService.get(context, "audit.member.warn.evade")));

        Mono<?> muteEvade = member.getGuild().flatMap(Guild::getOwner)
                .filterWhen(ignored -> adminService.isMuted(member))
                .flatMap(owner -> adminService.mute(owner, member, DateTime.now().plus(config.muteBaseDelay()),
                        messageService.get(context, "audit.member.mute.evade"))
                        .thenReturn(owner))
                .switchIfEmpty(warn.then(Mono.empty()));

        return auditService.log(event.getGuildId(), USER_JOIN)
                .withUser(member)
                .save()
                .and(muteEvade)
                .contextWrite(context);
    }

    @Override
    public Publisher<?> onMemberLeave(MemberLeaveEvent event){
        User user = event.getUser();
        if(DiscordUtil.isBot(user)){
            return Mono.empty();
        }
        Context context = Context.of(KEY_LOCALE, entityRetriever.getLocale(event.getGuildId()),
                KEY_TIMEZONE, entityRetriever.getTimeZone(event.getGuildId()));

        Mono<Void> log = auditService.log(event.getGuildId(), USER_LEAVE)
                .withUser(user)
                .save();

        Mono<Void> kick = event.getGuild()
                .flatMapMany(guild -> guild.getAuditLog(spec -> spec.setActionType(ActionType.MEMBER_KICK)))
                .filter(entry -> entry.getId().getTimestamp().isAfter(Instant.now().minusMillis(TIMEOUT_MILLIS)) &&
                        entry.getTargetId().map(target -> target.equals(user.getId())).orElse(false))
                .next()
                .flatMap(entry -> event.getGuild().flatMap(guild -> guild.getMemberById(entry.getResponsibleUserId()))
                        .flatMap(admin -> auditService.log(event.getGuildId(), USER_KICK)
                                .withUser(admin)
                                .withTargetUser(user)
                                .withAttribute(REASON, entry.getReason()
                                        .orElse(messageService.get(context, "common.not-defined")))
                                .save())
                        .thenReturn(entry))
                .switchIfEmpty(log.then(Mono.empty()))
                .then();

        return event.getGuild()
                .flatMapMany(guild -> guild.getAuditLog(spec -> spec.setActionType(ActionType.MEMBER_BAN_ADD)))
                .filter(entry -> entry.getId().getTimestamp().isAfter(Instant.now().minusMillis(TIMEOUT_MILLIS)) &&
                        entry.getTargetId().map(target -> target.equals(user.getId())).orElse(false))
                .next()
                .flatMap(entry -> event.getGuild().flatMap(guild -> guild.getMemberById(entry.getResponsibleUserId()))
                        .flatMap(admin -> auditService.log(event.getGuildId(), USER_BAN)
                                .withUser(admin)
                                .withTargetUser(user)
                                .withAttribute(REASON, entry.getReason()
                                        .orElse(messageService.get(context, "common.not-defined")))
                                .save())
                        .thenReturn(entry))
                .switchIfEmpty(kick.then(Mono.empty()))
                .contextWrite(context);
    }

    @Override
    public Publisher<?> onMemberUpdate(MemberUpdateEvent event){
        return event.getMember()
                .filter(DiscordUtil::isNotBot)
                .flatMap(member -> Mono.fromRunnable(() -> {
                    LocalMember localMember = entityRetriever.getMember(member);
                    localMember.effectiveName(event.getCurrentNickname().orElse(member.getUsername()));
                    entityRetriever.save(localMember);
                }));
    }
}
