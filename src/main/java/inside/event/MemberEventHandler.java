package inside.event;

import discord4j.core.event.domain.guild.*;
import discord4j.core.object.audit.*;
import discord4j.core.object.entity.*;
import inside.event.audit.*;
import inside.data.entity.*;
import inside.data.service.*;
import inside.util.*;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.time.Instant;
import java.util.*;

import static inside.event.audit.AuditEventType.*;
import static inside.util.ContextUtil.*;

@Component
public class MemberEventHandler extends AuditEventHandler{

    @Autowired
    private DiscordEntityRetrieveService discordEntityRetrieveService;

    @Override
    public Publisher<?> onBan(BanEvent event){ // не триггерится, баг д4ж текущей версии
        User user = event.getUser();
        if(DiscordUtil.isBot(user)) return Mono.empty();

        context = Context.of(KEY_GUILD_ID, event.getGuildId(),
                             KEY_LOCALE, discordEntityRetrieveService.locale(event.getGuildId()),
                             KEY_TIMEZONE, discordEntityRetrieveService.timeZone(event.getGuildId()));

        return log(event.getGuildId(), embed -> {
            embed.setColor(userBan.color);
            embed.setTitle(messageService.get(context, "audit.member.ban.title"));
            embed.setDescription(messageService.format(context, "audit.member.ban.description", user.getUsername()));
            embed.setFooter(timestamp(), null);
        });
    }

    @Override
    public Publisher<?> onMemberJoin(MemberJoinEvent event){
        Member member = event.getMember();
        if(DiscordUtil.isBot(member)) return Mono.empty();

        context = Context.of(KEY_GUILD_ID, event.getGuildId(),
                             KEY_LOCALE, discordEntityRetrieveService.locale(event.getGuildId()),
                             KEY_TIMEZONE, discordEntityRetrieveService.timeZone(event.getGuildId()));

        discordEntityRetrieveService.getMember(member, () -> new LocalMember(member));

        return log(event.getGuildId(), embed -> {
            embed.setColor(userJoin.color);
            embed.setTitle(messageService.get(context, "audit.member.join.title"));
            embed.setDescription(messageService.format(context, "audit.member.join.description", member.getUsername()));
            embed.setFooter(timestamp(), null);
        });
    }

    @Override
    public Publisher<?> onMemberLeave(MemberLeaveEvent event){
        User user = event.getUser();
        if(DiscordUtil.isBot(user)) return Mono.empty();
        context = Context.of(KEY_GUILD_ID, event.getGuildId(),
                             KEY_LOCALE, discordEntityRetrieveService.locale(event.getGuildId()),
                             KEY_TIMEZONE, discordEntityRetrieveService.timeZone(event.getGuildId()));

        AuditLogEntry l = event.getGuild().flatMapMany(g -> g.getAuditLog(q -> q.setActionType(ActionType.MEMBER_KICK))).blockFirst();
        if(l != null && l.getId().getTimestamp().isAfter(Instant.now().minusMillis(2500))){
            return event.getGuild().flatMap(g -> g.getMemberById(l.getResponsibleUserId()))
                    .flatMap(admin -> log(event.getGuildId(), embed -> {
                            embed.setColor(userKick.color);
                            embed.setTitle(messageService.get(context, "audit.member.kick.title"));
                            StringBuilder desc = new StringBuilder();
                            desc.append(messageService.format(context, "audit.member.kick.description", user.getUsername(), admin.getUsername()));
                            Optional<String> reason = l.getReason();
                            desc.append("\n").append(messageService.format(context, "common.reason", reason.filter(r -> !r.trim().isBlank()).isPresent() ? reason.map(String::trim).get() : messageService.get(context, "common.not-defined")));
                            embed.setDescription(desc.toString());
                            embed.setFooter(timestamp(), null);
                    }));
    ////}else if(){
        }else{
            return log(event.getGuildId(), embed -> {
                embed.setColor(userLeave.color);
                embed.setTitle(messageService.get(context, "audit.member.leave.title"));
                embed.setDescription(messageService.format(context, "audit.member.leave.description", user.getUsername()));
                embed.setFooter(timestamp(), null);
            });
        }
    }

    @Override
    public Publisher<?> onMemberUpdate(MemberUpdateEvent event){
        return event.getMember()
                    .filter(DiscordUtil::isNotBot)
                    .doOnNext(member -> discordEntityRetrieveService.save(discordEntityRetrieveService.getMember(member, () -> new LocalMember(member))))
                    .then();
    }
}
