package inside.command.common;

import discord4j.core.spec.MessageEditSpec;
import inside.command.Command;
import inside.command.model.*;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

@DiscordCommand(key = "ping", description = "command.ping.description")
public class PingCommand extends Command{
    @Override
    public Publisher<?> execute(CommandEnvironment env, CommandInteraction interaction){
        long start = System.currentTimeMillis();
        return env.channel().createMessage(messageService.get(env.context(), "command.ping.testing"))
                .flatMap(message -> message.edit(MessageEditSpec.builder()
                        .contentOrNull(messageService.format(env.context(), "command.ping.completed",
                                System.currentTimeMillis() - start))
                        .build()))
                .then();
    }
}
