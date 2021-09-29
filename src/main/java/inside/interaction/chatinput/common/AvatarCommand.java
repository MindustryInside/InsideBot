package inside.interaction.chatinput.common;

import discord4j.core.object.command.*;
import discord4j.core.retriever.EntityRetrievalStrategy;
import discord4j.core.spec.EmbedCreateSpec;
import inside.Settings;
import inside.interaction.*;
import inside.interaction.chatinput.*;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

@InteractionDiscordCommand(name = "avatar", description = "Get user avatar.")
public class AvatarCommand extends BaseInteractionCommand{

    @Autowired
    private Settings settings;

    public AvatarCommand(){

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
                .flatMap(user -> env.event().reply()
                        .withEmbeds(EmbedCreateSpec.builder()
                                .color(settings.getDefaults().getNormalColor())
                                .description(messageService.format(env.context(), "command.avatar.text",
                                        user.getUsername(), user.getMention()))
                                .image(user.getAvatarUrl() + "?size=512")
                                .build()));
    }
}
