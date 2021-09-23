package inside.interaction.component;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import reactor.core.publisher.Mono;

public interface ButtonListener{

    Mono<Void> handle(ButtonInteractionEvent event);
}
