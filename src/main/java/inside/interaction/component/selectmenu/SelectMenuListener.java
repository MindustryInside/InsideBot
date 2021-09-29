package inside.interaction.component.selectmenu;

import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import inside.interaction.component.*;
import reactor.core.publisher.Mono;

public interface SelectMenuListener extends InteractionListener{

    Mono<Void> handle(InteractionSelectMenuEnvironment env);
}
