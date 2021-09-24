package inside.interaction;

import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.*;
import reactor.core.publisher.Mono;

import java.util.List;

public interface InteractionCommand{

    Mono<Boolean> filter(InteractionCommandEnvironment env);

    Mono<Void> execute(InteractionCommandEnvironment env);

    String getName();

    String getDescription();

    ApplicationCommandOption.Type getType();

    List<ApplicationCommandOptionData> getOptions();

    ApplicationCommandRequest getRequest();
}
