package inside.interaction.user;

import discord4j.discordjson.json.ApplicationCommandRequest;
import inside.data.service.EntityRetriever;
import inside.interaction.UserEnvironment;
import inside.interaction.annotation.UserCommand;
import inside.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

public abstract class BaseUserInteractionCommand implements UserInteractionCommand{
    @Autowired
    protected MessageService messageService;

    @Autowired
    protected EntityRetriever entityRetriever;

    private final UserCommand metadata = getClass().getDeclaredAnnotation(UserCommand.class);

    @Override
    public Mono<Boolean> filter(UserEnvironment env){
        return Mono.just(true);
    }

    @Override
    public Mono<Void> execute(UserEnvironment env){
        return Mono.empty();
    }

    @Override
    public String getName(){
        return metadata.name();
    }

    @Override
    public ApplicationCommandRequest getRequest(){
        return ApplicationCommandRequest.builder()
                .name(getName())
                .type(getCommandType().getValue())
                .build();
    }
}
