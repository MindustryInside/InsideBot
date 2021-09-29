package inside.interaction.component.button;

import inside.interaction.component.*;
import reactor.core.publisher.Mono;

public interface ButtonListener extends InteractionListener{

    Mono<Void> handle(InteractionButtonEnvironment env);
}
