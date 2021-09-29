package inside.command.common;

import discord4j.core.retriever.EntityRetrievalStrategy;
import discord4j.core.spec.EmbedCreateSpec;
import inside.Settings;
import inside.command.Command;
import inside.command.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

@DiscordCommand(key = {"emoji", "emote"}, params = "command.emoji.params", description = "command.emoji.description")
public class EmojiCommand extends Command{

    @Autowired
    private Settings settings;

    @Override
    public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
        String text = interaction.getOption(0)
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asString)
                .orElseThrow(IllegalStateException::new);

        return env.member().getGuild()
                .flatMapMany(guild -> guild.getEmojis(EntityRetrievalStrategy.REST))
                .filter(emoji -> emoji.asFormat().equals(text) || emoji.getName().equals(text) ||
                        emoji.getId().asString().equals(text)).next()
                .switchIfEmpty(messageService.err(env, "command.emoji.not-found").then(Mono.empty()))
                .flatMap(emoji -> messageService.info(env, "command.emoji.text", emoji.getName(), emoji.asFormat())
                        .withEmbeds(EmbedCreateSpec.builder()
                                .color(settings.getDefaults().getNormalColor())
                                .image(emoji.getImageUrl() + "?size=512")
                                .footer(messageService.format(env.context(), "common.id", emoji.getId().asString()), null)
                                .build()))
                .then();
    }
}
