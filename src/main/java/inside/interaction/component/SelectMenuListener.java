package inside.interaction.component;

import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import reactor.core.publisher.Mono;

public interface SelectMenuListener extends InteractionListener{

    Mono<Void> handle(SelectMenuInteractionEvent event);
}
