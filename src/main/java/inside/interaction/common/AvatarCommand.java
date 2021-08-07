package inside.interaction.common;

import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.retriever.EntityRetrievalStrategy;
import discord4j.rest.util.ApplicationCommandOptionType;
import inside.interaction.*;
import reactor.core.publisher.Mono;

@InteractionDiscordCommand(name = "avatar", description = "Get user avatar.")
public class AvatarCommand extends BaseInteractionCommand{

    public AvatarCommand(){

        addOption(builder -> builder.name("target")
                .description("Whose avatar needs to get. By default your avatar")
                .type(ApplicationCommandOptionType.USER.getValue())
                .required(false));
    }

    @Override
    public Mono<Void> execute(InteractionCommandEnvironment env){
        return env.event().getOption("target")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(opt -> env.getClient().withRetrievalStrategy(EntityRetrievalStrategy.REST)
                        .getUserById(opt.asSnowflake()))
                .orElse(Mono.just(env.event().getInteraction().getUser()))
                .flatMap(user -> messageService.info(env.event(), embed -> embed.image(user.getAvatarUrl() + "?size=512")
                        .description(messageService.format(env.context(), "command.avatar.text", user.getUsername(),
                                user.getMention()))));
    }
}
