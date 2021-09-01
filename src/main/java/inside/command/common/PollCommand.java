package inside.command.common;

import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.*;
import discord4j.rest.util.*;
import inside.Settings;
import inside.command.Command;
import inside.command.model.*;
import inside.data.entity.GuildConfig;
import inside.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.*;

import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.*;

@DiscordCommand(key = "poll", params = "command.poll.params", description = "command.poll.description",
        permissions = {Permission.SEND_MESSAGES, Permission.EMBED_LINKS, Permission.ADD_REACTIONS})
public class PollCommand extends Command{

    public static final ReactionEmoji[] emojis;

    static{
        emojis = new ReactionEmoji[]{
                ReactionEmoji.unicode("1\u20E3"),
                ReactionEmoji.unicode("2\u20E3"),
                ReactionEmoji.unicode("3\u20E3"),
                ReactionEmoji.unicode("3\u20E3"),
                ReactionEmoji.unicode("4\u20E3"),
                ReactionEmoji.unicode("5\u20E3"),
                ReactionEmoji.unicode("6\u20E3"),
                ReactionEmoji.unicode("7\u20E3"),
                ReactionEmoji.unicode("8\u20E3"),
                ReactionEmoji.unicode("9\u20E3"),
                ReactionEmoji.unicode("\uD83D\uDD1F")
        };
    }

    @Autowired
    private Settings settings;

    @Override
    public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
        Mono<MessageChannel> channel = env.getReplyChannel();
        Member author = env.getAuthorAsMember();

        String text = interaction.getOption("poll text")
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asString)
                .orElseThrow(IllegalStateException::new);

        String[] vars = text.split("(?<!\\\\)" + Pattern.quote(","));
        String title = vars.length > 0 ? vars[0] : null;
        if(Strings.isEmpty(title)){
            return messageService.err(env, "command.poll.title").then(Mono.empty());
        }

        int count = vars.length - 1;
        if(count <= 0){
            return messageService.err(env, "command.poll.empty-variants");
        }

        if(count > emojis.length){
            return messageService.err(env, "common.limit-number", emojis.length - 1);
        }

        return channel.flatMap(reply -> reply.createMessage(MessageCreateSpec.builder()
                        .allowedMentions(AllowedMentions.suppressAll())
                        .addEmbed(EmbedCreateSpec.builder()
                                .title(title)
                                .color(settings.getDefaults().getNormalColor())
                                .description(IntStream.range(1, vars.length)
                                        .mapToObj(i -> String.format("**%d**. %s%n", i, vars[i]))
                                        .collect(Collectors.joining()))
                                .author(author.getUsername(), null, author.getAvatarUrl())
                                .build())
                        .build()))
                .flatMap(poll -> Mono.defer(() -> Flux.fromArray(emojis)
                        .take(count, true)
                        .flatMap(poll::addReaction)
                        .then(Mono.just(poll))))
                .then();
    }

    @Override
    public Mono<Void> help(CommandEnvironment env, String prefix){
        return messageService.infoTitled(env, "command.help.title", "command.poll.help",
                GuildConfig.formatPrefix(prefix));
    }
}
