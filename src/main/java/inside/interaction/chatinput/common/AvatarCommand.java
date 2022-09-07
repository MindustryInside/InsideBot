package inside.interaction.chatinput.common;

import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.retriever.EntityRetrievalStrategy;
import discord4j.core.spec.EmbedCreateSpec;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.annotation.ChatInputCommand;
import inside.interaction.chatinput.InteractionCommand;
import inside.service.MessageService;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

@ChatInputCommand(name = "commands.avatar.key", description = "commands.avatar.desc2")
public class AvatarCommand extends InteractionCommand {

    public AvatarCommand(MessageService messageService) {
        super(messageService);

        addOption(builder -> builder.name("target")
                .description(messageService.get(null,"command.avatar.params-target"))
                .type(ApplicationCommandOption.Type.USER.getValue())
                .required(false));
    }

    @Override
    public Publisher<?> execute(ChatInputInteractionEnvironment env) {
        return env.getOption("target")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(opt -> env.event().getClient().withRetrievalStrategy(EntityRetrievalStrategy.REST)
                        .getUserById(opt.asSnowflake()))
                .orElse(Mono.just(env.event().getInteraction().getUser()))
                .flatMap(user -> env.event().reply()
                        .withEmbeds(EmbedCreateSpec.builder()
                                .description(String.format(messageService.get(null,"command.avatar.message"), user.getUsername(), user.getMention()))
                                .color(env.configuration().discord().embedColor())
                                .image(user.getAvatarUrl() + "?size=512")
                                .build()));
    }
}
