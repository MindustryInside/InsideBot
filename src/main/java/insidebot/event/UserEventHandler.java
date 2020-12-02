package insidebot.event;

import discord4j.core.event.domain.UserUpdateEvent;
import discord4j.core.object.entity.User;
import insidebot.event.audit.AuditEventHandler;
import insidebot.data.entity.LocalUser;
import insidebot.data.service.UserService;
import insidebot.util.DiscordUtil;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class UserEventHandler extends AuditEventHandler{
    @Autowired
    private UserService userService;

    @Override
    public Publisher<?> onUserUpdate(UserUpdateEvent event){
        User user = event.getCurrent();
        if(DiscordUtil.isBot(user)) return Mono.empty();

        LocalUser info = userService.getOr(user.getId(), () -> new LocalUser(user));
        info.name(user.getUsername());
        userService.save(info);
        return Mono.empty();
    }
}
