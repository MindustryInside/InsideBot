package insidebot.common.command.model.base;

import discord4j.core.event.domain.message.MessageCreateEvent;
import insidebot.common.command.service.BaseCommandHandler.Command;
import insidebot.common.services.ContextService;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

public abstract class CommandRunner{
    @Autowired
    protected ContextService context;

    public abstract Mono<Void> execute(CommandReference reference, MessageCreateEvent event, String[] args);

    public Command compile(){
        DiscordCommand a = getClass().getDeclaredAnnotation(DiscordCommand.class);
        return new Command(a.key(), a.params(), a.description(), this, a.permissions());
    }
}
