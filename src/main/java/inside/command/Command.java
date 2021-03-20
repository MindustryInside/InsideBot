package inside.command;

import inside.command.model.*;
import inside.data.service.*;
import inside.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.util.function.Function;

public abstract class Command implements Function<CommandRequest, Mono<Boolean>>{
    @Autowired
    protected MessageService messageService;

    @Autowired
    protected EntityRetriever entityRetriever;

    @Override
    public Mono<Boolean> apply(CommandRequest req){
        return Mono.just(true);
    }

    public Mono<Void> execute(CommandEnvironment env, String[] args){
        return Mono.empty();
    }

    protected DiscordCommand getAnnotation(){
        return getClass().getDeclaredAnnotation(DiscordCommand.class);
    }
}
