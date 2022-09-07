package inside.interaction.chatinput.guild;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.spec.EmbedCreateSpec;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.annotation.ChatInputCommand;
import inside.interaction.chatinput.InteractionGuildCommand;
import inside.service.MessageService;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

@ChatInputCommand(name = "commands.emoji.name", description = "commands.emoji.desc")
public class EmojiCommand extends InteractionGuildCommand {

    public EmojiCommand(MessageService messageService) {
        super(messageService);

        addOption(builder -> builder.name("emoji")
                .description(messageService.get(null,"commands.emoji.params-emoji"))
                .required(true)
                .type(ApplicationCommandOption.Type.STRING.getValue()));
    }

    @Override
    public Publisher<?> execute(ChatInputInteractionEnvironment env) {

        Snowflake guildId = env.event().getInteraction().getGuildId().orElseThrow();

        String emojistr = env.getOption("emoji")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElseThrow();

        return env.event().getClient().getGuildEmojis(guildId)
                .filter(emoji -> emoji.asFormat().equals(emojistr) || emoji.getName().equals(emojistr) ||
                        emoji.getId().asString().equals(emojistr))
                .next()
                .switchIfEmpty(messageService.err(env, messageService.get(null,"commands.emoji.incorrect-format")).then(Mono.empty()))
                .flatMap(emoji -> env.event().reply()
                        .withEmbeds(EmbedCreateSpec.builder()
                                .color(env.configuration().discord().embedColor())
                                .image(emoji.getImageUrl() + "?size=512")
                                .description(String.format(messageService.get(null,"commands.emoji.header"), emoji.getName(), emoji.asFormat()))
                                .footer(String.format("ID: %s", emoji.getId().asString()), null)
                                .build()))
                .then();
    }
}
