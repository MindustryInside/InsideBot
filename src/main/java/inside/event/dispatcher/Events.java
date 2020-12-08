package inside.event.dispatcher;

import inside.event.audit.*;
import inside.event.dispatcher.EventType.*;
import org.reactivestreams.Publisher;
import org.slf4j.*;
import reactor.core.publisher.Mono;

/**
 * Сборка всех ивентов
 */
public abstract class Events extends AuditEventHandler{
    protected static final Logger log = LoggerFactory.getLogger(Events.class);

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
        if (event instanceof MessageClearEvent e) return onMessageClear(e);
        else if (event instanceof MemberUnmuteEvent e) return onMemberUnmute(e);
        else if (event instanceof MemberMuteEvent e) return onMemberMute(e);
        else return Mono.empty();
    }
}
