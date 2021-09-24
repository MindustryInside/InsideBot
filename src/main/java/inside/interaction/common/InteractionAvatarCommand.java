package inside.interaction.common;

import discord4j.core.object.command.*;
import discord4j.core.retriever.EntityRetrievalStrategy;
import inside.interaction.*;
import reactor.core.publisher.Mono;

@InteractionDiscordCommand(name = "avatar", description = "Get user avatar.")
public class InteractionAvatarCommand extends BaseInteractionCommand{

    public InteractionAvatarCommand(){

        addOption(builder -> builder.name("target")
                .description("Whose avatar needs to get. By default your avatar")
                .type(ApplicationCommandOption.Type.USER.getValue())
                .required(false));
    }

    @Override
    public Mono<Void> execute(InteractionCommandEnvironment env){
        return env.getOption("target")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(opt -> env.getClient().withRetrievalStrategy(EntityRetrievalStrategy.REST)
                        .getUserById(opt.asSnowflake()))
                .orElse(Mono.just(env.event().getInteraction().getUser()))
                .flatMap(user -> messageService.info(env.event(), embed -> embed.image(user.getAvatarUrl() + "?size=512")
                        .description(messageService.format(env.context(), "command.avatar.text", user.getUsername(),
                                user.getMention()))));
    }
}
