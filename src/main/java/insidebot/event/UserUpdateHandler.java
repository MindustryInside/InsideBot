package insidebot.event;

import discord4j.core.event.domain.UserUpdateEvent;
import discord4j.core.object.entity.User;
import insidebot.common.services.DiscordService;
import insidebot.data.entity.*;
import insidebot.data.service.*;
import insidebot.util.DiscordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class UserUpdateHandler implements EventHandler<UserUpdateEvent>{
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

        LocalUser info = userService.getById(user.getId());
        info.name(user.getUsername());
        userService.save(info);
        return Mono.empty();
    }
}
