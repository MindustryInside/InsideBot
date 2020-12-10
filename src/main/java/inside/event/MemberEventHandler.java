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

import java.time.Instant;
import java.util.*;

import static inside.event.audit.AuditEventType.*;

@Component
public class MemberEventHandler extends AuditEventHandler{
    @Autowired
    private UserService userService;

    @Autowired
    private MemberService memberService;

    @Override
    public Publisher<?> onBan(BanEvent event){ // не триггерится, баг д4ж текущей версии
        User user = event.getUser();
        if(DiscordUtil.isBot(user)) return Mono.empty();
        context.init(event.getGuildId());

        return log(event.getGuildId(), embed -> {
            embed.setColor(userBan.color);
            embed.setTitle(messageService.get("audit.member.ban.title"));
            embed.setDescription(messageService.format("audit.member.ban.description", user.getUsername()));
            embed.setFooter(MessageUtil.zonedFormat(), null);
        })
        .thenEmpty(Mono.fromRunnable(() -> memberService.deleteById(event.getGuildId(), user.getId())));
    }

    @Override
    public Publisher<?> onMemberJoin(MemberJoinEvent event){
        Member member = event.getMember();
        if(DiscordUtil.isBot(member)) return Mono.empty();
        context.init(member.getGuildId());

        memberService.getOr(member, () -> {
            LocalMember l = new LocalMember(member);
            l.user(userService.getOr(member.getId(), () -> new LocalUser(member)));
            return l;
        });

        return log(event.getGuildId(), embed -> {
            embed.setColor(userJoin.color);
            embed.setTitle(messageService.get("audit.member.join.title"));
            embed.setDescription(messageService.format("audit.member.join.description", member.getUsername()));
            embed.setFooter(MessageUtil.zonedFormat(), null);
        });
    }

    @Override
    public Publisher<?> onMemberLeave(MemberLeaveEvent event){
        User user = event.getUser();
        if(DiscordUtil.isBot(user)) return Mono.empty();
        context.init(event.getGuildId());
        AuditLogEntry l = event.getGuild().flatMapMany(g -> g.getAuditLog(q -> q.setActionType(ActionType.MEMBER_KICK))).blockFirst();
        memberService.deleteById(event.getGuildId(), user.getId());
        if(l != null && l.getId().getTimestamp().isAfter(Instant.now().minusMillis(2500))){
            Member moderator = event.getGuild().flatMap(g -> g.getMemberById(l.getResponsibleUserId())).block();
            if(moderator == null) return Mono.empty();
            return log(event.getGuildId(), embed -> {
                embed.setColor(userKick.color);
                embed.setTitle(messageService.get("audit.member.kick.title"));
                StringBuilder desc = new StringBuilder();
                desc.append(messageService.format("audit.member.kick.description", user.getUsername(), moderator.getUsername()));
                Optional<String> reason = l.getReason();
                desc.append('\n').append(messageService.format("common.reason", reason.filter(r -> !r.trim().isBlank()).isPresent() ? reason.map(String::trim).get() : messageService.get("common.not-defined")));
                embed.setDescription(desc.toString());
                embed.setFooter(MessageUtil.zonedFormat(), null);
            });
        }else{
            return log(event.getGuildId(), embed -> {
                embed.setColor(userLeave.color);
                embed.setTitle(messageService.get("audit.member.leave.title"));
                embed.setDescription(messageService.format("audit.member.leave.description", user.getUsername()));
                embed.setFooter(MessageUtil.zonedFormat(), null);
            });
        }
    }

    @Override
    public Publisher<?> onMemberUpdate(MemberUpdateEvent event){
        return event.getMember()
                    .filter(DiscordUtil::isNotBot)
                    .doOnNext(member -> {
                        LocalMember localMember = memberService.get(member);
                        if(localMember != null){
                            if(localMember.user() == null){
                                User user = discordService.gateway().getUserById(member.getId()).block();
                                LocalUser local = new LocalUser(Objects.requireNonNull(user));
                                localMember.user(local);
                                userService.save(local);
                            }
                            localMember.effectiveName(member.getDisplayName());
                            memberService.save(localMember);
                        }
                    })
                    .then();
    }
}
