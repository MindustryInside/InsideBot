package inside.interaction.chatinput;

import discord4j.core.object.command.ApplicationCommand;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import inside.interaction.*;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.List;

public interface InteractionChatInputCommand extends InteractionCommand{

    Publisher<Boolean> filter(CommandEnvironment env);

    Publisher<?> execute(CommandEnvironment env);

    String getDescription();

    List<ApplicationCommandOptionData> getOptions();

    @Override
    default ApplicationCommand.Type getCommandType(){
        return ApplicationCommand.Type.CHAT_INPUT;
    }
}
