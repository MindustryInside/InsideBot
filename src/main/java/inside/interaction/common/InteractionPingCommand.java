package inside.interaction.common;

import inside.interaction.*;
import reactor.core.publisher.Mono;

@InteractionDiscordCommand(name = "ping1", description = "Get bot ping.")
public class InteractionPingCommand extends BaseInteractionCommand{
    @Override
    public Mono<Void> execute(InteractionCommandEnvironment env){
        long start = System.currentTimeMillis();
        return env.event().createFollowup(messageService.get(env.context(), "command.ping.testing"))
                .flatMap(message -> env.event().editFollowup(message.getId())
                        .withContentOrNull(messageService.format(env.context(), "command.ping.completed",
                                System.currentTimeMillis() - start)))
                .then();
    }
}
