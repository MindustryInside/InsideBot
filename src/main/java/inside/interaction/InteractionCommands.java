package inside.interaction;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.discordjson.json.ApplicationCommandRequest;
import inside.interaction.model.InteractionDiscordCommand;
import reactor.core.publisher.Mono;

public class InteractionCommands{

    private InteractionCommands(){}

    @InteractionDiscordCommand
    public static class PingCommand extends InteractionCommand{
        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){
            Mono<MessageChannel> reply = env.getClient().getChannelById(env.event().getInteraction().getChannelId())
                    .cast(MessageChannel.class);

            long start = System.currentTimeMillis();
            return env.event().acknowledge().then(env.event().getInteractionResponse()
                    .createFollowupMessage(messageService.get(env.context(), "command.ping.testing"))
                    .flatMap(data -> reply.flatMap(channel -> channel.getMessageById(Snowflake.of(data.id())))
                            .flatMap(message -> message.edit(spec -> spec.setContent(messageService.format(env.context(), "command.ping.completed",
                                    System.currentTimeMillis() - start))))))
                    .then();
        }

        @Override
        public ApplicationCommandRequest getRequest(){
            return ApplicationCommandRequest.builder()
                    .name("ping")
                    .description("Get bot ping.")
                    .build();
        }
    }
}
