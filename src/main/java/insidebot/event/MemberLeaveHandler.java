package insidebot.event;

import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.object.entity.User;
import discord4j.core.spec.MessageCreateSpec;
import insidebot.audit.AuditEventHandler;
import insidebot.common.services.DiscordService;
import insidebot.data.service.*;
import insidebot.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static insidebot.InsideBot.logChannelID;
import static insidebot.audit.AuditEventType.*;

@Component
public class MemberLeaveHandler extends AuditEventHandler<MemberLeaveEvent>{
    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @Autowired
    private DiscordService discordService;

    @Override
    public Class<MemberLeaveEvent> type(){
        return MemberLeaveEvent.class;
    }

    @Override
    public Mono<Void> onEvent(MemberLeaveEvent event){
        User user = event.getUser();

        Mono<Void> publishLog = log(embedBuilder -> {
            embedBuilder.setColor(userLeave.color);
            embedBuilder.setTitle(messageService.get("message.user-leave"));
            embedBuilder.setDescription(messageService.format("message.user-leave.text", user.getUsername()));
            embedBuilder.setFooter(MessageUtil.zonedFormat(), null);
        });

        return Mono.justOrEmpty(user)
                   .flatMap(u -> !DiscordUtil.isBot(u) ? publishLog : Mono.empty())
                   .thenEmpty(Mono.fromRunnable(() -> userService.deleteById(user.getId())));
    }

    @Override
    public Mono<Void> log(MessageCreateSpec message){
        return discordService.getTextChannelById(logChannelID)
                             .flatMap(c -> c.getRestChannel().createMessage(message.asRequest()))
                             .then();
    }
}
