package inside.event.dispatcher;

import inside.event.audit.*;
import inside.event.dispatcher.EventType.*;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.util.*;
import reactor.util.context.Context;

import static inside.util.ContextUtil.*;

public abstract class Events extends AuditEventHandler{
    protected static final Logger log = Loggers.getLogger(Events.class);

    public Publisher<?> onMessageClear(MessageClearEvent event){
        return Mono.empty();
    }

    public Publisher<?> onMemberUnmute(MemberUnmuteEvent event){
        return Mono.empty();
    }

    public Publisher<?> onMemberMute(MemberMuteEvent event){
        return Mono.empty();
    }

    public final Publisher<?> hookOnEvent(BaseEvent event){
        context = Context.of(KEY_GUILD_ID, event.guild.getId(),
                             KEY_LOCALE, discordEntityRetrieveService.locale(event.guild.getId()),
                             KEY_TIMEZONE, discordEntityRetrieveService.timeZone(event.guild.getId()));

        if (event instanceof MessageClearEvent e) return onMessageClear(e);
        else if (event instanceof MemberUnmuteEvent e) return onMemberUnmute(e);
        else if (event instanceof MemberMuteEvent e) return onMemberMute(e);
        else return Mono.empty();
    }
}
