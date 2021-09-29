package inside.interaction.chatinput;

import inside.interaction.*;
import reactor.core.publisher.Mono;

import java.util.List;

public interface InteractionCommandHandler{

    Mono<Void> handleChatInputCommand(InteractionCommandEnvironment env);

    Mono<Void> handleUserCommand(InteractionUserEnvironment env);
}
