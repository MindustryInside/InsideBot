package insidebot.event;

import discord4j.core.event.domain.Event;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Mono;

public interface EventHandled<T extends Event>{
    Class<T> type();

    Mono<Void> onEvent(@NonNull T event);
}
