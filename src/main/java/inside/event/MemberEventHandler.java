package inside.event;

import discord4j.core.event.domain.guild.*;
import discord4j.core.object.audit.ActionType;
import discord4j.core.object.entity.*;
import inside.data.entity.LocalMember;
import inside.data.service.*;
import inside.event.audit.AuditEventHandler;
import inside.event.dispatcher.EventType;
import inside.util.*;
import org.joda.time.DateTime;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.context.Context;

import java.time.Instant;
import java.util.Optional;

import static inside.event.audit.AuditEventType.*;
import static inside.util.ContextUtil.*;

@Component
public class MemberEventHandler extends AuditEventHandler{
    public static final long TIMEOUT_MILLIS = 2500L;

    @Autowired
    private EntityRetriever entityRetriever;

    @Autowired
    private AdminService adminService;

    @Override
    @Deprecated
    public Publisher<?> onBan(BanEvent event){ // не триггерится, баг д4ж текущей версии
        User user = event.getUser();
        if(DiscordUtil.isBot(user)) return Mono.empty();

        context = Context.of(KEY_GUILD_ID, event.getGuildId(),
                             KEY_LOCALE, entityRetriever.locale(event.getGuildId()),
                             KEY_TIMEZONE, entityRetriever.timeZone(event.getGuildId()));

        return log(event.getGuildId(), embed -> embed.setColor(userBan.color)
                .setTitle(messageService.get(context, "audit.member.ban.title"))
                .setDescription(messageService.format(context, "audit.member.ban.description", user.getUsername()))
                .setFooter(timestamp(), null));
    }

    @Override
    public Publisher<?> onMemberJoin(MemberJoinEvent event){
        Member member = event.getMember();
        if(DiscordUtil.isBot(member)) return Mono.empty();

        context = Context.of(KEY_GUILD_ID, event.getGuildId(),
                             KEY_LOCALE, entityRetriever.locale(event.getGuildId()),
                             KEY_TIMEZONE, entityRetriever.timeZone(event.getGuildId()));

        LocalMember localMember = entityRetriever.getMember(member, () -> new LocalMember(member));

        Mono<Void> warn = adminService.warnings(localMember.guildId(), localMember.userId()).count()
                .filter(c -> c >= 3)
                .flatMap(__ -> member.getGuild().flatMap(Guild::getOwner).map(owner -> entityRetriever.getMember(owner, () -> new LocalMember(owner))))
                .flatMap(owner -> Mono.fromRunnable(() ->
                        adminService.warn(owner, localMember, messageService.get(context, "audit.member.warn.evade"))
                ));

        Mono<Void> muteEvade = adminService.isMuted(member.getGuildId(), member.getId())
                .zipWith(member.getGuild().flatMap(Guild::getOwner).map(owner -> entityRetriever.getMember(owner, () -> new LocalMember(owner))))
                .flatMap(TupleUtils.function((bool, owner) -> bool ? member.getGuild().flatMap(guild -> Mono.fromRunnable(() -> discordService.eventListener().publish(
                        new EventType.MemberMuteEvent(guild, owner, localMember, messageService.get(context, "audit.member.mute.evade"), DateTime.now().plusDays(10))
                ))) : Mono.empty()))
                .switchIfEmpty(warn)
                .then();

        return log(event.getGuildId(), embed -> embed.setColor(userJoin.color)
                .setTitle(messageService.get(context, "audit.member.join.title"))
                .setDescription(messageService.format(context, "audit.member.join.description", member.getUsername()))
                .setFooter(timestamp(), null))
                .then(muteEvade);
    }

    @Override
    public Publisher<?> onMemberLeave(MemberLeaveEvent event){
        User user = event.getUser();
        if(DiscordUtil.isBot(user)) return Mono.empty();
        context = Context.of(KEY_GUILD_ID, event.getGuildId(),
                             KEY_LOCALE, entityRetriever.locale(event.getGuildId()),
                             KEY_TIMEZONE, entityRetriever.timeZone(event.getGuildId()));

        Mono<Void> log = log(event.getGuildId(), embed -> embed.setColor(userLeave.color)
                .setTitle(messageService.get(context, "audit.member.leave.title"))
                .setDescription(messageService.format(context, "audit.member.leave.description", user.getUsername()))
                .setFooter(timestamp(), null));

        Mono<Void> kick = event.getGuild().flatMapMany(g -> g.getAuditLog(q -> q.setActionType(ActionType.MEMBER_KICK)))
                .filter(entry -> entry.getId().getTimestamp().isAfter(Instant.now().minusMillis(TIMEOUT_MILLIS)))
                .flatMap(entry -> event.getGuild().flatMap(guild -> guild.getMemberById(entry.getResponsibleUserId()))
                        .flatMap(admin -> log(event.getGuildId(), embed -> embed.setColor(userKick.color)
                                .setTitle(messageService.get(context, "audit.member.kick.title"))
                                .setDescription(String.format("%s%n%s",
                                messageService.format(context, "audit.member.kick.description", user.getUsername(), admin.getUsername()),
                                messageService.format(context, "common.reason", entry.getReason().filter(MessageUtil::isNotEmpty).map(String::trim).orElse(messageService.get(context, "common.not-defined")))
                                ))
                                .setFooter(timestamp(), null)
                        )))
                .then()
                .switchIfEmpty(log);

        return event.getGuild().flatMapMany(guild -> guild.getAuditLog(q -> q.setActionType(ActionType.MEMBER_BAN_ADD)))
                .filter(entry -> entry.getId().getTimestamp().isAfter(Instant.now().minusMillis(TIMEOUT_MILLIS)))
                .flatMap(entry -> event.getGuild().flatMap(guild -> guild.getMemberById(entry.getResponsibleUserId()))
                        .flatMap(admin -> log(event.getGuildId(), embed -> embed.setColor(userKick.color)
                                .setTitle(messageService.get(context, "audit.member.ban.title"))
                                .setDescription(String.format("%s%n%s",
                                messageService.format(context, "audit.member.ban.description", user.getUsername(), admin.getUsername()),
                                messageService.format(context, "common.reason", entry.getReason().filter(MessageUtil::isNotEmpty).map(String::trim).orElse(messageService.get(context, "common.not-defined")))
                                ))
                                .setFooter(timestamp(), null)
                        )))
                .then()
                .switchIfEmpty(kick);
    }

    @Override
    public Publisher<?> onMemberUpdate(MemberUpdateEvent event){
        return event.getMember()
                .filter(DiscordUtil::isNotBot)
                .doOnNext(member -> {
                    LocalMember localMember = entityRetriever.getMember(member, () -> new LocalMember(member));
                    event.getCurrentNickname().ifPresent(localMember::effectiveName);
                    entityRetriever.save(localMember);
                })
                .then();
    }
}
