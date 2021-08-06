package inside.interaction;

import discord4j.discordjson.json.*;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.util.List;

public interface InteractionCommand{

    Mono<Boolean> filter(InteractionCommandEnvironment env);

    Mono<Void> execute(InteractionCommandEnvironment env);

    String getName();

    String getDescription();

    ApplicationCommandOptionType getType();

    List<ApplicationCommandOptionData> getOptions();

    ApplicationCommandRequest getRequest();
}
