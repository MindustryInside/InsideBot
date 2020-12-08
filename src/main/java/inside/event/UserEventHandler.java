package inside.event;

import discord4j.core.event.domain.UserUpdateEvent;
import inside.event.audit.AuditEventHandler;
import inside.data.entity.LocalUser;
import inside.data.service.UserService;
import inside.util.DiscordUtil;
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
        return Mono.just(event.getCurrent())
                .filter(DiscordUtil::isNotBot)
                .doOnNext(u -> {
                    LocalUser localUser = userService.getOr(u.getId(), () -> new LocalUser(u));
                    localUser.name(u.getUsername());
                    userService.save(localUser);
                })
                .then();
    }
}
