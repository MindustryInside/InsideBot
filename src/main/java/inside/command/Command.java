package inside.command;

import inside.service.MessageService;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Objects;

public abstract class Command {
    protected final MessageService messageService;

    public Command(MessageService messageService) {
        this.messageService = Objects.requireNonNull(messageService);
    }

    public Publisher<Boolean> filter(CommandEnvironment env) {
        return Mono.just(true);
    }

    public Publisher<?> execute(CommandEnvironment env, CommandInteraction interaction) {
        return Mono.empty();
    }

    protected DiscordCommand getAnnotation() {
        return getClass().getDeclaredAnnotation(DiscordCommand.class);
    }
}
