package inside.interaction.chatinput.common;

import inside.interaction.*;
import inside.interaction.annotation.ChatInputCommand;
import inside.interaction.chatinput.*;
import reactor.core.publisher.Mono;

@ChatInputCommand(name = "ping", description = "Get bot ping.")
public class PingCommand extends BaseInteractionCommand{
    @Override
    public Mono<Void> execute(CommandEnvironment env){
        long start = System.currentTimeMillis();
        return env.event().deferReply().then(
                env.event().createFollowup(messageService.get(env.context(), "command.ping.testing")))
                .flatMap(message -> env.event().editFollowup(message.getId())
                        .withContentOrNull(messageService.format(env.context(), "command.ping.completed",
                                System.currentTimeMillis() - start)))
                .then();
    }
}
