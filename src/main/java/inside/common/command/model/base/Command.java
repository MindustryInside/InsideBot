package inside.common.command.model.base;

import reactor.core.publisher.Mono;

public abstract class Command{

    public abstract Mono<Void> execute(CommandReference reference, String[] args);

    public CommandInfo compile(){
        DiscordCommand annotation = getClass().getDeclaredAnnotation(DiscordCommand.class);
        return new CommandInfo(annotation.key(), annotation.params(), annotation.description(), annotation.permissions());
    }
}
