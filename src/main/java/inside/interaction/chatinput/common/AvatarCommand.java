package inside.interaction.chatinput.common;

import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandOption.Type;
import discord4j.core.retriever.EntityRetrievalStrategy;
import discord4j.core.spec.EmbedCreateSpec;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.annotation.ChatInputCommand;
import inside.interaction.chatinput.InteractionCommand;
import inside.service.MessageService;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

@ChatInputCommand("commands.common.avatar")
public class AvatarCommand extends InteractionCommand {

    public AvatarCommand(MessageService messageService) {
        super(messageService);

        addOption("target", s -> s.type(Type.USER.getValue()).required(false));
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
                                .description(messageService.format(env.context(), "commands.common.avatar.format",
                                        user.getUsername(), user.getMention()))
                                .color(env.configuration().discord().embedColor())
                                .image(user.getAvatarUrl() + "?size=512")
                                .build()));
    }
}
