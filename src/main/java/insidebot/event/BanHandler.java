package insidebot.event;

import discord4j.core.event.domain.guild.BanEvent;
import discord4j.core.object.entity.User;
import discord4j.core.spec.MessageCreateSpec;
import insidebot.common.services.DiscordService;
import insidebot.data.services.*;
import insidebot.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static insidebot.InsideBot.logChannelID;
import static insidebot.audit.AuditEventType.userBan;

@Component
public class BanHandler extends AuditEventHandler<BanEvent>{
    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @Autowired
    private DiscordService discordService;

    @Override
    public Class<BanEvent> type(){
        return BanEvent.class;
    }

    @Override
    public Mono<Void> onEvent(BanEvent event){
        User user = event.getUser();

        Mono<Void> publishLog = log(embedBuilder -> {
            embedBuilder.setColor(userBan.color);
            embedBuilder.setTitle(messageService.get("message.ban"));
            embedBuilder.setDescription(messageService.format("message.ban.text", user.getUsername()));
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
