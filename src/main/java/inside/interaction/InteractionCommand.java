package inside.interaction;

import discord4j.discordjson.json.ApplicationCommandRequest;
import inside.data.service.EntityRetriever;
import inside.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.util.function.Function;

public abstract class InteractionCommand implements Function<InteractionCommandEnvironment, Mono<Boolean>>{

    @Autowired
    protected MessageService messageService;

    @Autowired
    protected EntityRetriever entityRetriever;

    @Override
    public Mono<Boolean> apply(InteractionCommandEnvironment env){
        return Mono.just(true);
    }

    public Mono<Void> execute(InteractionCommandEnvironment env){
        return Mono.empty();
    }

    public abstract ApplicationCommandRequest getRequest();
}
