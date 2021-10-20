package inside.interaction.user;

import discord4j.core.object.command.*;
import inside.interaction.*;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

public interface UserInteractionCommand extends InteractionCommand{

    Publisher<Boolean> filter(UserEnvironment env);

    Publisher<?> execute(UserEnvironment env);

    @Override
    default ApplicationCommandOption.Type getType(){
        return ApplicationCommandOption.Type.UNKNOWN;
    }

    @Override
    default ApplicationCommand.Type getCommandType(){
        return ApplicationCommand.Type.USER;
    }
}
