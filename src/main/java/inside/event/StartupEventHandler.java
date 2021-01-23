package inside.event;

import discord4j.core.event.domain.lifecycle.ReadyEvent;
import inside.event.audit.AuditEventHandler;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.*;

@Component
public class StartupEventHandler extends AuditEventHandler{
    private static final Logger log = Loggers.getLogger(MessageEventHandler.class);

    @Override
    public Publisher<?> onReady(ReadyEvent event){ // не триггерится, баг текущей версии d4j
        return Mono.fromRunnable(() -> log.info("Bot up."));
    }
}
