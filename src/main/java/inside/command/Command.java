package inside.command;

import inside.command.model.*;
import inside.data.service.EntityRetriever;
import inside.service.MessageService;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

public abstract class Command{
    @Autowired
    protected MessageService messageService;

    @Autowired
    protected EntityRetriever entityRetriever;

    public Publisher<Boolean> filter(CommandEnvironment env){
        return Mono.just(true);
    }

    public Publisher<?> execute(CommandEnvironment env, CommandInteraction interaction){
        return Mono.empty();
    }

    public Publisher<?> help(CommandEnvironment env, String prefix){
        return messageService.text(env, "command.help.default")
                .then();
    }

    protected DiscordCommand getAnnotation(){
        return getClass().getDeclaredAnnotation(DiscordCommand.class);
    }
}
