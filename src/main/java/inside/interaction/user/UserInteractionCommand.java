package inside.interaction.user;

import discord4j.core.object.command.*;
import inside.interaction.*;
import reactor.core.publisher.Mono;

public interface UserInteractionCommand extends InteractionCommand{

    Mono<Boolean> filter(UserEnvironment env);

    Mono<Void> execute(UserEnvironment env);

    @Override
    default ApplicationCommandOption.Type getType(){
        return ApplicationCommandOption.Type.UNKNOWN;
    }

    @Override
    default ApplicationCommand.Type getCommandType(){
        return ApplicationCommand.Type.USER;
    }
}
