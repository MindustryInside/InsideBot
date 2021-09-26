package inside.interaction.component;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import reactor.core.publisher.Mono;

public interface ButtonListener extends InteractionListener{

    Mono<Void> handle(ButtonInteractionEvent event);
}
