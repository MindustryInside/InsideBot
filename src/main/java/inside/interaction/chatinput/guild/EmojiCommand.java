package inside.interaction.chatinput.guild;

import discord4j.common.util.Snowflake;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption.Type;
import discord4j.core.spec.EmbedCreateSpec;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.annotation.ChatInputCommand;
import inside.interaction.annotation.Option;
import inside.interaction.chatinput.InteractionGuildCommand;
import inside.service.MessageService;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

@ChatInputCommand("commands.common.emoji")
@Option(name = "emoji", type = Type.STRING, required = true)
public class EmojiCommand extends InteractionGuildCommand {

    public EmojiCommand(MessageService messageService) {
        super(messageService);
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
                .switchIfEmpty(messageService.err(env, "commands.common.emoji.invalid").then(Mono.empty()))
                .flatMap(emoji -> env.event().reply()
                        .withEmbeds(EmbedCreateSpec.builder()
                                .color(env.configuration().discord().embedColor())
                                .image(emoji.getImageUrl() + "?size=512")
                                .description(messageService.format(env.context(), "commands.common.emoji.format",
                                        emoji.getName(), emoji.asFormat()))
                                .footer(messageService.format(env.context(), "common.id", emoji.getId().asString()), null)
                                .build()))
                .then();
    }
}
