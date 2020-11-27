package insidebot.event;

import discord4j.core.event.domain.UserUpdateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.spec.MessageCreateSpec;
import insidebot.common.services.DiscordService;
import insidebot.data.entity.UserInfo;
import insidebot.data.services.*;
import insidebot.util.DiscordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static insidebot.InsideBot.logChannelID;

@Component
public class UserUpdateHandler extends AuditEventHandler<UserUpdateEvent>{
    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @Autowired
    private DiscordService discordService;

    @Override
    public Class<UserUpdateEvent> type(){
        return UserUpdateEvent.class;
    }

    @Override
    public Mono<Void> onEvent(UserUpdateEvent event){
        User user = event.getCurrent();
        if(DiscordUtil.isBot(user)) return Mono.empty();
        if(!userService.exists(user.getId())) return Mono.empty();

        UserInfo info = userService.getById(user.getId());
        info.name(user.getUsername());
        userService.save(info);
        return Mono.empty();
    }

    @Override
    public Mono<Void> log(MessageCreateSpec message){
        return discordService.getTextChannelById(logChannelID)
                             .flatMap(c -> c.getRestChannel().createMessage(message.asRequest()))
                             .then();
    }
}
