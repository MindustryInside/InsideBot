package inside.common.command.model.base;

import inside.data.service.*;
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

    public Mono<Void> execute(CommandReference reference, String[] args){
        return Mono.empty();
    }

    public CommandInfo compile(){
        DiscordCommand annotation = getClass().getDeclaredAnnotation(DiscordCommand.class);
        return new CommandInfo(annotation.key(), annotation.params(), annotation.description(), annotation.permissions());
    }
}
