package insidebot.event;

import discord4j.core.event.domain.Event;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

public interface EventHandler<T extends Event>{
    Class<T> type();

    Mono<Void> onEvent(@NonNull T event);
}
