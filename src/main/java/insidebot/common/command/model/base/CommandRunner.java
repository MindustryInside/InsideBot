package insidebot.common.command.model.base;

import discord4j.core.event.domain.message.MessageCreateEvent;
import insidebot.common.command.service.BaseCommandHandler.Command;
import insidebot.data.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

public interface CommandRunner{

    Mono<Void> execute(CommandReference reference, MessageCreateEvent event, String[] args);

    default Command compile(){
        DiscordCommand a = getClass().getDeclaredAnnotation(DiscordCommand.class);
        return new Command(a.key(), a.params(), a.description(), this);
    }
}
