package inside.command;

import inside.command.model.*;
import inside.data.service.EntityRetriever;
import inside.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.util.function.Function;

public abstract class Command implements Function<CommandEnvironment, Mono<Boolean>>{
    @Autowired
    protected MessageService messageService;

    @Autowired
    protected EntityRetriever entityRetriever;

    @Override
    public Mono<Boolean> apply(CommandEnvironment env){
        return Mono.just(true);
    }

    public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
        return Mono.empty();
    }

    public Mono<Void> help(CommandEnvironment env){
        return messageService.text(env.getReplyChannel(), "command.help.default");
    }

    protected DiscordCommand getAnnotation(){
        return getClass().getDeclaredAnnotation(DiscordCommand.class);
    }
}
