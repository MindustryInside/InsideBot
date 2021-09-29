package inside.interaction.user;

import discord4j.core.object.command.*;
import inside.interaction.*;
import reactor.core.publisher.Mono;

public interface UserCommand extends InteractionCommand{

    Mono<Boolean> filter(InteractionUserEnvironment env);

    Mono<Void> execute(InteractionUserEnvironment env);

    @Override
    default ApplicationCommandOption.Type getType(){
        return ApplicationCommandOption.Type.UNKNOWN;
    }

    @Override
    default ApplicationCommand.Type getCommandType(){
        return ApplicationCommand.Type.USER;
    }
}
