package inside.event;

import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.guild.*;
import discord4j.core.object.audit.ActionType;
import discord4j.core.object.entity.*;
import inside.Settings;
import inside.data.entity.LocalMember;
import inside.data.service.*;
import inside.event.audit.AuditService;
import inside.util.*;
import org.joda.time.DateTime;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.context.Context;

import java.time.Instant;

import static inside.event.audit.AuditActionType.*;
import static inside.event.audit.BaseAuditProvider.KEY_REASON;
import static inside.util.ContextUtil.*;

@Component
public class MemberEventHandler extends ReactiveEventAdapter{
    public static final long TIMEOUT_MILLIS = 2500L;

    @Autowired
    private EntityRetriever entityRetriever;

    @Autowired
    private AuditService auditService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private Settings settings;

    @Override
    public Publisher<?> onMemberJoin(MemberJoinEvent event){
        Member member = event.getMember();
        if(DiscordUtil.isBot(member)) return Mono.empty();

        Context context = Context.of(KEY_GUILD_ID, event.getGuildId(), KEY_LOCALE, entityRetriever.locale(event.getGuildId()), KEY_TIMEZONE, entityRetriever.timeZone(event.getGuildId()));

        LocalMember localMember = entityRetriever.getMember(member);

        Mono<Void> warn = member.getGuild().flatMap(Guild::getOwner).map(entityRetriever::getMember)
                .filterWhen(owner -> adminService.warnings(localMember.guildId(), localMember.userId()).count().map(c -> c >= settings.maxWarnings))
                .flatMap(owner -> adminService.warn(owner, localMember, messageService.get(context, "audit.member.warn.evade")));

        Mono<?> muteEvade = adminService.isMuted(member.getGuildId(), member.getId())
                .zipWith(member.getGuild().flatMap(Guild::getOwner).map(entityRetriever::getMember))
                .flatMap(TupleUtils.function((bool, owner) -> bool ? adminService.mute(
                        owner, localMember, DateTime.now().plusDays(settings.muteEvadeDays).toCalendar(entityRetriever.locale(localMember.guildId())), messageService.get(context, "audit.member.mute.evade")
                ).thenReturn(owner) : Mono.empty()))
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
        if(DiscordUtil.isBot(user)) return Mono.empty();
        Context context = Context.of(KEY_GUILD_ID, event.getGuildId(), KEY_LOCALE, entityRetriever.locale(event.getGuildId()), KEY_TIMEZONE, entityRetriever.timeZone(event.getGuildId()));

        Mono<Void> log = auditService.log(event.getGuildId(), USER_LEAVE)
                .withUser(user)
                .save();

        Mono<Void> kick = event.getGuild().flatMapMany(guild -> guild.getAuditLog(spec -> spec.setActionType(ActionType.MEMBER_KICK)))
                .filter(entry -> entry.getId().getTimestamp().isAfter(Instant.now().minusMillis(TIMEOUT_MILLIS)))
                .flatMap(entry -> event.getGuild().flatMap(guild -> guild.getMemberById(entry.getResponsibleUserId()))
                        .flatMap(admin -> auditService.log(event.getGuildId(), USER_KICK)
                                .withUser(admin)
                                .withTargetUser(user)
                                .withAttribute(KEY_REASON, entry.getReason()
                                        .filter(MessageUtil::isNotEmpty)
                                        .orElse(messageService.get(context, "common.not-defined")))
                                .save())
                        .thenReturn(entry))
                .switchIfEmpty(log.then(Mono.empty()))
                .then();

        return event.getGuild().flatMapMany(guild -> guild.getAuditLog(spec -> spec.setActionType(ActionType.MEMBER_BAN_ADD)))
                .filter(entry -> entry.getId().getTimestamp().isAfter(Instant.now().minusMillis(TIMEOUT_MILLIS)))
                .flatMap(entry -> event.getGuild().flatMap(guild -> guild.getMemberById(entry.getResponsibleUserId()))
                        .flatMap(admin -> auditService.log(event.getGuildId(), USER_BAN)
                                .withUser(admin)
                                .withTargetUser(user)
                                .withAttribute(KEY_REASON, entry.getReason()
                                        .filter(MessageUtil::isNotEmpty)
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
                    event.getCurrentNickname().ifPresent(nickname -> {
                        localMember.effectiveName(nickname);
                        entityRetriever.save(localMember);
                    });
                }));
    }
}
