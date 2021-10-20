package inside.interaction.chatinput;

import inside.interaction.*;
import reactor.core.publisher.Mono;

public interface InteractionCommandHandler{

    Mono<Void> handleChatInputCommand(CommandEnvironment env);

    Mono<Void> handleUserCommand(UserEnvironment env);
}
