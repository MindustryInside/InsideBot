package inside.interaction;

import reactor.core.publisher.Mono;

import java.util.List;

public interface InteractionCommandHandler{

    Mono<Void> handle(InteractionCommandEnvironment env);

    List<InteractionCommand> getCommands();
}
