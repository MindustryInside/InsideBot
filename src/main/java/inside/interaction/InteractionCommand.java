package inside.interaction;

import discord4j.discordjson.json.ApplicationCommandRequest;
import inside.data.service.EntityRetriever;
import inside.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

public abstract class InteractionCommand{
    @Autowired
    protected MessageService messageService;

    @Autowired
    protected EntityRetriever entityRetriever;

    public Mono<Boolean> filter(InteractionCommandEnvironment env){
        return Mono.just(true);
    }

    public Mono<Void> execute(InteractionCommandEnvironment env){
        return Mono.empty();
    }

    public abstract ApplicationCommandRequest getRequest();
}
