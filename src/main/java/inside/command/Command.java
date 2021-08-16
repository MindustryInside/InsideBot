package inside.command;

import inside.command.model.*;
import inside.data.service.EntityRetriever;
import inside.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

public abstract class Command{
    @Autowired
    protected MessageService messageService;

    @Autowired
    protected EntityRetriever entityRetriever;

    public Mono<Boolean> filter(CommandEnvironment env){
        return Mono.just(true);
    }

    public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
        return Mono.empty();
    }

    public Mono<Void> help(CommandEnvironment env, String prefix){
        return messageService.text(env, "command.help.default");
    }

    protected DiscordCommand getAnnotation(){
        return getClass().getDeclaredAnnotation(DiscordCommand.class);
    }
}
