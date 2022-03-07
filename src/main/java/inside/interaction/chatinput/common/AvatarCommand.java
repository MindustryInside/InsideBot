package inside.interaction.chatinput.common;

import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.retriever.EntityRetrievalStrategy;
import discord4j.core.spec.EmbedCreateSpec;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.annotation.ChatInputCommand;
import inside.interaction.chatinput.InteractionCommand;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.text.MessageFormat;

@ChatInputCommand(name = "avatar", description = "Получить аватар указанного пользователя.")
public class AvatarCommand extends InteractionCommand {

    public AvatarCommand() {

        addOption(builder -> builder.name("target")
                .description("Пользователь, чей аватар нужно получить. По умолчанию отправляю ваш аватар")
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
                                .description(MessageFormat.format("Аватар **%s** (%s):", user.getUsername(), user.getMention()))
                                .color(env.configuration().discord().embedColor())
                                .image(user.getAvatarUrl() + "?size=512")
                                .build()));
    }
}
