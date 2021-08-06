package inside.interaction.common;

import discord4j.core.object.entity.Message;
import discord4j.discordjson.json.WebhookMessageEditRequest;
import reactor.core.publisher.Mono;

@InteractionDiscordCommand(name = "ping", description = "Get bot ping.")
public class PingCommand extends InteractionCommand{
    @Override
    public Mono<Void> execute(InteractionCommandEnvironment env){
        long start = System.currentTimeMillis();
        return env.event().acknowledge().then(env.event().getInteractionResponse()
                .createFollowupMessage(messageService.get(env.context(), "command.ping.testing"))
                .map(data -> new Message(env.getClient(), data))
                .flatMap(message -> env.event().getInteractionResponse()
                        .editFollowupMessage(message.getId().asLong(), WebhookMessageEditRequest.builder()
                                .contentOrNull(messageService.format(env.context(), "command.ping.completed",
                                        System.currentTimeMillis() - start))
                                .build(), true))
                .then());
    }
}
