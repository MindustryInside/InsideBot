package inside.command.common;

import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.rest.util.*;
import inside.command.Command;
import inside.command.model.*;
import inside.data.entity.GuildConfig;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

@DiscordCommand(key = "qpoll", params = "command.qpoll.params", description = "command.qpoll.description",
        permissions = {Permission.SEND_MESSAGES, Permission.EMBED_LINKS, Permission.ADD_REACTIONS})
public class QuickPollCommand extends Command{
    public static final ReactionEmoji up = ReactionEmoji.unicode("\uD83D\uDC4D");
    public static final ReactionEmoji down = ReactionEmoji.unicode("\uD83D\uDC4E");

    @Override
    public Publisher<?> execute(CommandEnvironment env, CommandInteraction interaction){
        String text = interaction.getOption(0)
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asString)
                .orElseThrow();

        return env.channel().createMessage(messageService.format(
                env.context(), "command.qpoll.text", env.member().getUsername(), text))
                .flatMap(message1 -> message1.addReaction(up)
                        .and(message1.addReaction(down)));
    }

    @Override
    public Publisher<?> help(CommandEnvironment env, String prefix){
        return messageService.infoTitled(env, "command.help.title", "command.qpoll.help", GuildConfig.formatPrefix(prefix));
    }
}
