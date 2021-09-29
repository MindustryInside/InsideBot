package inside.interaction.user;

import discord4j.discordjson.json.ApplicationCommandRequest;
import inside.data.service.EntityRetriever;
import inside.interaction.InteractionUserEnvironment;
import inside.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

public abstract class BaseUserCommand implements UserCommand{
    @Autowired
    protected MessageService messageService;

    @Autowired
    protected EntityRetriever entityRetriever;

    private final InteractionUserCommand metadata = getClass().getDeclaredAnnotation(InteractionUserCommand.class);

    @Override
    public Mono<Boolean> filter(InteractionUserEnvironment env){
        return Mono.just(true);
    }

    @Override
    public Mono<Void> execute(InteractionUserEnvironment env){
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
