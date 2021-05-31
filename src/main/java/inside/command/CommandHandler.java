package inside.command;

import inside.command.model.CommandEnvironment;
import reactor.core.publisher.Mono;

public interface CommandHandler{

    Mono<Void> handleMessage(CommandEnvironment environment);
}
