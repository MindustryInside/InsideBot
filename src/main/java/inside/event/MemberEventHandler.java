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
import reactor.util.context.Context;

import java.time.Instant;
import java.util.Optional;

import static inside.event.audit.AuditEventType.*;
import static inside.util.ContextUtil.*;

@Component
public class MemberEventHandler extends AuditEventHandler{

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

        Mono<Long> warns = adminService.warnings(localMember.guildId(), localMember.userId()).count();
        Mono<Void> evade = Mono.defer(() -> adminService.get(AdminService.AdminActionType.mute, localMember.guildId(), localMember.userId()).next())
                .zipWith(warns)
                .flatMap(t -> t.getT2() >= 3
                              ? adminService.warn(localMember, localMember, messageService.get(context, "audit.member.warn.evade"))
                              : member.getGuild().flatMap(guild -> Mono.fromRunnable(() -> discordService.eventListener().publish(
                                    new EventType.MemberMuteEvent(guild, t.getT1().admin(), localMember, messageService.get(context, "audit.member.mute.evade"), new DateTime(t.getT1().end()))
                              )))
                );

        Mono<Void> log = log(event.getGuildId(), embed -> embed.setColor(userJoin.color)
                .setTitle(messageService.get(context, "audit.member.join.title"))
                .setDescription(messageService.format(context, "audit.member.join.description", member.getUsername()))
                .setFooter(timestamp(), null));

        return log.then(evade);
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
                .filter(entry -> entry.getId().getTimestamp().isAfter(Instant.now().minusMillis(2500)))
                .flatMap(entry -> event.getGuild().flatMap(guild -> guild.getMemberById(entry.getResponsibleUserId()))
                        .flatMap(admin -> log(event.getGuildId(), embed -> {
                            embed.setColor(userKick.color);
                            embed.setTitle(messageService.get(context, "audit.member.kick.title"));
                            StringBuilder desc = new StringBuilder();
                            desc.append(messageService.format(context, "audit.member.kick.description", user.getUsername(), admin.getUsername()));
                            Optional<String> reason = entry.getReason();
                            desc.append("\n").append(messageService.format(context, "common.reason", reason.filter(MessageUtil::isNotEmpty).map(String::trim).orElse(messageService.get(context, "common.not-defined"))));
                            embed.setDescription(desc.toString());
                            embed.setFooter(timestamp(), null);
                        })))
                .then()
                .switchIfEmpty(log);

        return event.getGuild().flatMapMany(guild -> guild.getAuditLog(q -> q.setActionType(ActionType.MEMBER_BAN_ADD)))
                .filter(entry -> entry.getId().getTimestamp().isAfter(Instant.now().minusMillis(2500)))
                .flatMap(entry -> event.getGuild().flatMap(guild -> guild.getMemberById(entry.getResponsibleUserId()))
                        .flatMap(admin -> log(event.getGuildId(), embed -> {
                            embed.setColor(userKick.color);
                            embed.setTitle(messageService.get(context, "audit.member.ban.title"));
                            StringBuilder desc = new StringBuilder();
                            desc.append(messageService.format(context, "audit.member.ban.description", user.getUsername(), admin.getUsername()));
                            Optional<String> reason = entry.getReason();
                            desc.append("\n").append(messageService.format(context, "common.reason", reason.filter(MessageUtil::isNotEmpty).map(String::trim).orElse(messageService.get(context, "common.not-defined"))));
                            embed.setDescription(desc.toString());
                            embed.setFooter(timestamp(), null);
                        })))
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
