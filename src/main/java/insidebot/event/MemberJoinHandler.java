package insidebot.event;

import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.object.entity.User;
import discord4j.core.spec.MessageCreateSpec;
import insidebot.audit.AuditEventHandler;
import insidebot.common.services.DiscordService;
import insidebot.data.service.MessageService;
import insidebot.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static insidebot.InsideBot.logChannelID;
import static insidebot.audit.AuditEventType.userJoin;

@Component
public class MemberJoinHandler extends AuditEventHandler<MemberJoinEvent>{
    @Autowired
    private MessageService messageService;

    @Autowired
    private DiscordService discordService;

    @Override
    public Class<MemberJoinEvent> type(){
        return MemberJoinEvent.class;
    }

    @Override
    public Mono<Void> onEvent(MemberJoinEvent event){
        User user = discordService.gateway().getUserById(event.getMember().getId()).block();

        Mono<Void> publishLog = log(embedBuilder -> {
            embedBuilder.setColor(userJoin.color);
            embedBuilder.setTitle(messageService.get("message.user-join"));
            embedBuilder.setDescription(messageService.format("message.user-join.text", user.getUsername()));
            embedBuilder.setFooter(MessageUtil.zonedFormat(), null);
        });

        return Mono.justOrEmpty(user).flatMap(u -> !DiscordUtil.isBot(u) ? publishLog : Mono.empty());
    }

    @Override
    public Mono<Void> log(MessageCreateSpec message){
        return discordService.getTextChannelById(logChannelID)
                             .flatMap(c -> c.getRestChannel().createMessage(message.asRequest()))
                             .then();
    }
}
