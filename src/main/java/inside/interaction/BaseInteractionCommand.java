package inside.interaction;

import discord4j.discordjson.json.*;
import discord4j.rest.util.ApplicationCommandOptionType;
import inside.data.service.EntityRetriever;
import inside.service.MessageService;
import inside.util.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Consumer;

public abstract class BaseInteractionCommand implements InteractionCommand{
    @Autowired
    protected MessageService messageService;

    @Autowired
    protected EntityRetriever entityRetriever;

    private final InteractionDiscordCommand metadata =
            getClass().getDeclaredAnnotation(InteractionDiscordCommand.class);
    private final List<ApplicationCommandOptionData> options = new ArrayList<>();

    @Override
    public Mono<Boolean> filter(InteractionCommandEnvironment env){
        return Mono.just(true);
    }

    @Override
    public Mono<Void> execute(InteractionCommandEnvironment env){
        return Mono.empty();
    }

    @Override
    public String getName(){
        return metadata.name();
    }

    @Override
    public String getDescription(){
        return metadata.description();
    }

    @Override
    public ApplicationCommandOptionType getType(){
        return metadata.type();
    }

    @Override
    public List<ApplicationCommandOptionData> getOptions(){
        return options;
    }

    @Override
    public ApplicationCommandRequest getRequest(){
        Preconditions.requireState(getType() == ApplicationCommandOptionType.UNKNOWN,
                "Subcommands mustn't define command request.");

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