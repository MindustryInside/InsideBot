package inside.event;

import discord4j.core.event.domain.guild.*;
import discord4j.core.object.audit.*;
import discord4j.core.object.entity.*;
import inside.Settings;
import inside.data.entity.*;
import inside.data.service.*;
import inside.event.audit.AuditEventHandler;
import inside.event.dispatcher.EventType;
import inside.util.*;
import org.joda.time.DateTime;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.*;
import reactor.function.TupleUtils;
import reactor.util.context.Context;

import java.time.Instant;

import static inside.event.audit.AuditActionType.*;
import static inside.util.ContextUtil.*;

@Component
public class MemberEventHandler extends AuditEventHandler{
    public static final long TIMEOUT_MILLIS = 2500L;

    @Autowired
    private EntityRetriever entityRetriever;

    @Autowired
    private AdminService adminService;

    @Autowired
    private Settings settings;

    @Override
    @Deprecated
    public Publisher<?> onBan(BanEvent event){
        User user = event.getUser();
        if(DiscordUtil.isBot(user)) return Mono.empty();

        Context context = Context.of(KEY_GUILD_ID, event.getGuildId(),
                                     KEY_LOCALE, entityRetriever.locale(event.getGuildId()),
                                     KEY_TIMEZONE, entityRetriever.timeZone(event.getGuildId()));

        return log(event.getGuildId(), embed -> embed.setColor(USER_BAN.color)
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

        LocalMember localMember = entityRetriever.getMember(member);

        Mono<Void> warn = member.getGuild().flatMap(Guild::getOwner).map(entityRetriever::getMember)
                .filterWhen(owner -> adminService.warnings(localMember.guildId(), localMember.userId()).count().map(c -> c >= settings.maxWarnings))
                .flatMap(owner -> adminService.warn(owner, localMember, messageService.get(context, "audit.member.warn.evade")));

        Mono<?> muteEvade = adminService.isMuted(member.getGuildId(), member.getId())
                .zipWith(member.getGuild().flatMap(Guild::getOwner).map(entityRetriever::getMember))
                .flatMap(TupleUtils.function((bool, owner) -> bool ? member.getGuild().flatMap(guild -> Mono.fromRunnable(() -> discordService.eventListener().publish(
                        new EventType.MemberMuteEvent(guild, owner, localMember, messageService.get(context, "audit.member.mute.evade"), DateTime.now().plusDays(settings.muteEvadeDays))
                ))).thenReturn(owner) : Mono.empty()))
                .switchIfEmpty(warn.then(Mono.empty()));

        return log(event.getGuildId(), embed -> embed.setColor(USER_JOIN.color)
                .setTitle(messageService.get(context, "audit.member.join.title"))
                .setDescription(messageService.format(context, "audit.member.join.description", member.getUsername()))
                .setFooter(timestamp(), null))
                .then(muteEvade);
    }

    @Override
    public Publisher<?> onMemberLeave(MemberLeaveEvent event){
        User user = event.getUser();
        if(DiscordUtil.isBot(user)) return Mono.empty();
        Context context = Context.of(KEY_GUILD_ID, event.getGuildId(), KEY_LOCALE, entityRetriever.locale(event.getGuildId()), KEY_TIMEZONE, entityRetriever.timeZone(event.getGuildId()));

        Mono<Void> log = log(event.getGuildId(), embed -> embed.setColor(USER_LEAVE.color)
                .setTitle(messageService.get(context, "audit.member.leave.title"))
                .setDescription(messageService.format(context, "audit.member.leave.description", user.getUsername()))
                .setFooter(timestamp(), null));

        Mono<Void> kick = event.getGuild().flatMapMany(guild -> guild.getAuditLog(spec -> spec.setActionType(ActionType.MEMBER_KICK)))
                .filter(entry -> entry.getId().getTimestamp().isAfter(Instant.now().minusMillis(TIMEOUT_MILLIS)))
                .flatMap(entry -> event.getGuild().flatMap(guild -> guild.getMemberById(entry.getResponsibleUserId()))
                        .flatMap(admin -> log(event.getGuildId(), embed -> embed.setColor(USER_KICK.color)
                                .setTitle(messageService.get(context, "audit.member.kick.title"))
                                .setDescription(String.format("%s%n%s",
                                messageService.format(context, "audit.member.kick.description", user.getUsername(), admin.getUsername()),
                                messageService.format(context, "common.reason", entry.getReason()
                                        .filter(MessageUtil::isNotEmpty)
                                        .orElse(messageService.get(context, "common.not-defined")))
                                ))
                                .setFooter(timestamp(), null)))
                        .thenReturn(entry))
                .switchIfEmpty(log.then(Mono.empty()))
                .then();

        return event.getGuild().flatMapMany(guild -> guild.getAuditLog(spec -> spec.setActionType(ActionType.MEMBER_BAN_ADD)))
                .filter(entry -> entry.getId().getTimestamp().isAfter(Instant.now().minusMillis(TIMEOUT_MILLIS)))
                .flatMap(entry -> event.getGuild().flatMap(guild -> guild.getMemberById(entry.getResponsibleUserId()))
                        .flatMap(admin -> log(event.getGuildId(), embed -> embed.setColor(USER_KICK.color)
                                .setTitle(messageService.get(context, "audit.member.ban.title"))
                                .setDescription(String.format("%s%n%s",
                                messageService.format(context, "audit.member.ban.description", user.getUsername(), admin.getUsername()),
                                messageService.format(context, "common.reason", entry.getReason()
                                        .filter(MessageUtil::isNotEmpty)
                                        .orElse(messageService.get(context, "common.not-defined")))
                                ))
                                .setFooter(timestamp(), null)))
                        .thenReturn(entry))
                .switchIfEmpty(kick.then(Mono.empty()));
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
