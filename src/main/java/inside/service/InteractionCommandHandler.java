package inside.service;

import inside.interaction.InteractionCommandEnvironment;
import reactor.core.publisher.Mono;

public interface InteractionCommandHandler{

    Mono<Void> handle(InteractionCommandEnvironment env);
}
