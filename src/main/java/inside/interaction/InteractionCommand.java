package inside.interaction;

import discord4j.discordjson.json.*;
import discord4j.rest.util.ApplicationCommandOptionType;
import inside.data.service.EntityRetriever;
import inside.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Consumer;

public abstract class InteractionCommand{
    @Autowired
    protected MessageService messageService;

    @Autowired
    protected EntityRetriever entityRetriever;

    private final InteractionDiscordCommand metadata =
            getClass().getDeclaredAnnotation(InteractionDiscordCommand.class);
    private final List<ApplicationCommandOptionData> options = new ArrayList<>();

    public Mono<Boolean> filter(InteractionCommandEnvironment env){
        return Mono.just(true);
    }

    public Mono<Void> execute(InteractionCommandEnvironment env){
        return Mono.empty();
    }

    public String getName(){
        return metadata.name();
    }

    public String getDescription(){
        return metadata.description();
    }

    public ApplicationCommandOptionType getType(){
        return metadata.type();
    }

    public List<ApplicationCommandOptionData> getOptions(){
        return options;
    }

    public ApplicationCommandRequest getRequest(){
        return ApplicationCommandRequest.builder()
                .name(metadata.name())
                .description(metadata.description())
                .options(getOptions())
                .build();
    }

    protected ApplicationCommandOptionData addOption(Consumer<? super ImmutableApplicationCommandOptionData.Builder> option){
        var mutatedOption = ApplicationCommandOptionData.builder();
        option.accept(mutatedOption);
        var build = mutatedOption.build();
        options.add(build);
        return build;
    }
}
