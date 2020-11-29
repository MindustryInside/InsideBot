package insidebot.event;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.guild.*;
import discord4j.core.object.entity.User;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.MessageData;
import insidebot.audit.AuditEventHandler;
import insidebot.data.entity.LocalMember;
import insidebot.data.service.*;
import insidebot.util.*;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static insidebot.audit.AuditEventType.*;

@Component
public class MemberEventHandler extends AuditEventHandler{
    @Autowired
    private UserService userService;

    @Autowired
    private MemberService memberService;

    @Override
    public Publisher<?> onBan(BanEvent event){
        User user = event.getUser();

        Mono<Void> publishLog = log(event.getGuildId(), embed-> {
            embed.setColor(userBan.color);
            embed.setTitle(messageService.get("message.ban"));
            embed.setDescription(messageService.format("message.ban.text", user.getUsername()));
            embed.setFooter(MessageUtil.zonedFormat(), null);
        });

        return Mono.justOrEmpty(user)
                   .flatMap(u -> !DiscordUtil.isBot(u) ? publishLog : Mono.empty())
                   .thenEmpty(Mono.fromRunnable(() -> userService.deleteById(user.getId())));
    }

    @Override
    public Publisher<?> onMemberJoin(MemberJoinEvent event){
        User user = discordService.gateway().getUserById(event.getMember().getId()).block();
        if(DiscordUtil.isBot(user)) return Mono.empty();

        return log(event.getGuildId(), embed -> {
            embed.setColor(userJoin.color);
            embed.setTitle(messageService.get("message.user-join"));
            embed.setDescription(messageService.format("message.user-join.text", user.getUsername()));
            embed.setFooter(MessageUtil.zonedFormat(), null);
        });
    }

    @Override
    public Publisher<?> onMemberLeave(MemberLeaveEvent event){
        User user = event.getUser();

        Mono<Void> publishLog = log(event.getGuildId(), embed -> {
            embed.setColor(userLeave.color);
            embed.setTitle(messageService.get("message.user-leave"));
            embed.setDescription(messageService.format("message.user-leave.text", user.getUsername()));
            embed.setFooter(MessageUtil.zonedFormat(), null);
        });

        return Mono.justOrEmpty(user)
                   .flatMap(u -> !DiscordUtil.isBot(u) ? publishLog : Mono.empty())
                   .thenEmpty(Mono.fromRunnable(() -> userService.deleteById(user.getId())));
    }

    @Override
    public Publisher<?> onMemberUpdate(MemberUpdateEvent event){
        return event.getMember()
                    .filter(m -> !DiscordUtil.isBot(m))
                    .filter(m -> memberService.exists(m.getGuildId(), m.getId()))
                    .doOnNext(m -> {
                        LocalMember info = memberService.get(event.getGuildId(), m.getId());
                        info.effectiveName(m.getUsername());
                        memberService.save(info);
                    })
                    .then();
    }

    @Override
    public Mono<Void> log(Snowflake guildId, MessageCreateSpec message){
        MessageData data = discordService.getLogChannel(guildId)
                                         .flatMap(c -> c.getRestChannel().createMessage(message.asRequest()))
                                         .block();
        return Mono.justOrEmpty(data).flatMap(__ -> Mono.fromRunnable(() -> context.reset()));
    }
}
