package insidebot.event;

import discord4j.core.event.domain.guild.*;
import discord4j.core.object.audit.*;
import discord4j.core.object.entity.*;
import insidebot.event.audit.*;
import insidebot.data.entity.*;
import insidebot.data.service.*;
import insidebot.util.*;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static insidebot.event.audit.AuditEventType.*;

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
            embed.setTitle(messageService.get("message.ban"));
            embed.setDescription(messageService.format("message.ban.text", user.getUsername()));
            embed.setFooter(MessageUtil.zonedFormat(), null);
        })
        .thenEmpty(Mono.fromRunnable(() -> memberService.deleteById(event.getGuildId(), user.getId())));
    }

    @Override
    public Publisher<?> onMemberJoin(MemberJoinEvent event){
        User user = discordService.gateway().getUserById(event.getMember().getId()).block();
        if(DiscordUtil.isBot(user)) return Mono.empty();
        context.init(event.getGuildId());

        LocalMember member = new LocalMember();
        member.guildId(event.getGuildId());
        member.effectiveName(event.getMember().getDisplayName());
        if(member.user() == null){
            LocalUser localUser = new LocalUser();
            localUser.name(user.getUsername());
            localUser.userId(user.getId());
            member.user(localUser);
            userService.save(localUser);
        }

        return log(event.getGuildId(), embed -> {
            embed.setColor(userJoin.color);
            embed.setTitle(messageService.get("message.user-join"));
            embed.setDescription(messageService.format("message.user-join.text", user.getUsername()));
            embed.setFooter(MessageUtil.zonedFormat(), null);
        })
        .thenEmpty(Mono.fromRunnable(() -> memberService.save(member)));
    }

    @Override
    public Publisher<?> onMemberLeave(MemberLeaveEvent event){
        User user = event.getUser();
        if(DiscordUtil.isBot(user)) return Mono.empty();
        context.init(event.getGuildId());
        AuditLogEntry l = event.getGuild().flatMapMany(Guild::getAuditLog)
                               .filter(a -> a.getActionType() == ActionType.MEMBER_KICK)
                               .filter(a -> user.getId().equals(a.getTargetId().orElse(null))).blockFirst();
        return Mono.justOrEmpty(l).flatMap(a -> {
            if(a != null && a.getId().getTimestamp().isAfter(Instant.now().minusMillis(2500))){
                Member moderator = event.getGuild().flatMap(g -> g.getMemberById(a.getResponsibleUserId())).block();
                if(moderator == null) return Mono.empty();
                return log(event.getGuildId(), embed -> {
                    embed.setColor(userKick.color);
                    embed.setTitle(messageService.get("message.user-kick"));
                    String desc = messageService.format("message.user-kick.text", user.getUsername(), moderator.getUsername());
                    if(a.getReason().isPresent() && !a.getReason().get().isBlank()){
                        desc += "\n" + messageService.format("message.reason", a.getReason().get().trim());
                    }
                    embed.setDescription(desc);
                    embed.setFooter(MessageUtil.zonedFormat(), null);
                });
            }else{
                return log(event.getGuildId(), embed -> {
                    embed.setColor(userLeave.color);
                    embed.setTitle(messageService.get("message.user-leave"));
                    embed.setDescription(messageService.format("message.user-leave.text", user.getUsername()));
                    embed.setFooter(MessageUtil.zonedFormat(), null);
                });
            }
        })
        .thenEmpty(Mono.fromRunnable(() -> memberService.deleteById(event.getGuildId(), user.getId())));
    }

    @Override
    public Publisher<?> onMemberUpdate(MemberUpdateEvent event){
        context.init(event.getGuildId());
        return event.getMember()
                    .filter(DiscordUtil::isNotBot)
                    .doOnNext(m -> {
                        LocalMember info = memberService.getOr(m, () -> new LocalMember(m));
                        info.effectiveName(m.getDisplayName());
                        if(info.user() == null){
                            LocalUser user = new LocalUser();
                            user.userId(m.getId());
                            user.name(m.getUsername());
                            info.user(user);
                            userService.save(user);
                        }
                        memberService.save(info);
                    })
                    .then();
    }
}
