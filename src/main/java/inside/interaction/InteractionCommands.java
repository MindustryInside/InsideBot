package inside.interaction;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.*;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.discordjson.json.*;
import discord4j.rest.util.ApplicationCommandOptionType;
import inside.Settings;
import inside.interaction.model.InteractionDiscordCommand;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

public class InteractionCommands{

    private InteractionCommands(){}

    @InteractionDiscordCommand
    public static class AvatarCommand extends InteractionCommand{
        @Autowired
        private Settings settings;

        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){
            return env.event().getInteraction().getCommandInteraction()
                    .getOption("target")
                    .flatMap(ApplicationCommandInteractionOption::getValue)
                    .map(ApplicationCommandInteractionOptionValue::asUser)
                    .orElse(Mono.just(env.event().getInteraction().getUser()))
                    .flatMap(avatar -> env.event().reply(spec -> spec.addEmbed(embed -> embed.setImage(avatar.getAvatarUrl() + "?size=512")
                            .setColor(settings.getDefaults().getNormalColor())
                            .setDescription(messageService.format(env.context(), "command.avatar.text", avatar.getUsername())))));
        }

        @Override
        public ApplicationCommandRequest getRequest(){
            return ApplicationCommandRequest.builder()
                    .name("avatar")
                    .description("Get user avatar.")
                    .addOption(ApplicationCommandOptionData.builder()
                            .name("target")
                            .type(ApplicationCommandOptionType.USER.getValue())
                            .required(false)
                            .description("Target user or self")
                            .build())
                    .build();
        }
    }

    @InteractionDiscordCommand
    public static class PingCommand extends InteractionCommand{
        @Override
        public Mono<Void> execute(InteractionCommandEnvironment env){
            Mono<MessageChannel> reply = env.getReplyChannel();

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
