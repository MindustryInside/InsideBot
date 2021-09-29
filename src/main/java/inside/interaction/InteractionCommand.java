package inside.interaction;

import discord4j.core.object.command.*;
import discord4j.discordjson.json.ApplicationCommandRequest;

public interface InteractionCommand{

    String getName();

    ApplicationCommandOption.Type getType();

    ApplicationCommand.Type getCommandType();

    ApplicationCommandRequest getRequest();
}
