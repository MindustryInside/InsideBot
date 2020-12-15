package inside.common.command.model.base;

import inside.common.services.ContextService;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

public abstract class Command{
    @Autowired
    protected ContextService context;

    public abstract Mono<Void> execute(CommandReference reference, String[] args);

    public CommandInfo compile(){
        DiscordCommand a = getClass().getDeclaredAnnotation(DiscordCommand.class);
        return new CommandInfo(a.key(), a.params(), a.description(), a.permissions());
    }
}
