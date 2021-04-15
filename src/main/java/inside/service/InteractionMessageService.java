package inside.service;

import discord4j.core.event.domain.InteractionCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

public interface InteractionMessageService{

    Mono<Void> text(InteractionCreateEvent event, String text, Object... args);

    Mono<Void> info(InteractionCreateEvent event, String title, String text, Object... args);

    Mono<Void> info(InteractionCreateEvent event, Consumer<EmbedCreateSpec> embed);

    Mono<Void> err(InteractionCreateEvent event, String text, Object... args);

    // TODO: rename
    Mono<Void> error(InteractionCreateEvent event, String title, String text, Object... args);
}
