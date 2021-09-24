package inside.command.common;

import discord4j.core.object.component.*;
import discord4j.core.object.entity.Member;
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
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.*;

@DiscordCommand(key = "poll", params = "command.poll.params", description = "command.poll.description",
        permissions = {Permission.SEND_MESSAGES, Permission.EMBED_LINKS})
public class PollCommand extends Command{

    public static final Button[] buttons;

    static{

        buttons = new Button[]{
                Button.primary("inside-poll-1", ReactionEmoji.unicode("1\u20E3")),
                Button.primary("inside-poll-2", ReactionEmoji.unicode("2\u20E3")),
                Button.primary("inside-poll-3", ReactionEmoji.unicode("3\u20E3")),
                Button.primary("inside-poll-4", ReactionEmoji.unicode("4\u20E3")),
                Button.primary("inside-poll-5", ReactionEmoji.unicode("5\u20E3")),
                Button.primary("inside-poll-6", ReactionEmoji.unicode("6\u20E3")),
                Button.primary("inside-poll-7", ReactionEmoji.unicode("7\u20E3")),
                Button.primary("inside-poll-8", ReactionEmoji.unicode("8\u20E3")),
                Button.primary("inside-poll-9", ReactionEmoji.unicode("9\u20E3")),
                Button.primary("inside-poll-10", ReactionEmoji.unicode("\uD83D\u20E3"))
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

        if(count > buttons.length){
            return messageService.err(env, "common.limit-number", buttons.length - 1);
        }

        List<LayoutComponent> rows = new ArrayList<>();
        List<ActionComponent> components = new ArrayList<>();
        for(int i = 0, count0 = count + 1; i < count0; i++){
            if(i != 0 && (i % 5 == 0 || i + 1 == count0)){ // action row can contain only 5 components
                rows.add(ActionRow.of(components));
                components.clear();
            }else{
                components.add(buttons[i]);
            }
        }

        return channel.flatMap(reply -> reply.createMessage(MessageCreateSpec.builder()
                        .allowedMentions(AllowedMentions.suppressAll())
                        .addEmbed(EmbedCreateSpec.builder()
                                .title(title)
                                .color(settings.getDefaults().getNormalColor())
                                .description(IntStream.range(1, vars.length)
                                        .mapToObj(i -> String.format("**%d**. %s%n", i, vars[i]))
                                        .collect(Collectors.joining()))
                                .author(author.getTag(), null, author.getAvatarUrl())
                                .build())
                        .components(rows)
                        .build()))
                .flatMap(message -> entityRetriever.createPoll(env.getAuthorAsMember().getGuildId(),
                        message.getId(), Arrays.asList(vars)))
                .then();
    }

    @Override
    public Mono<Void> help(CommandEnvironment env, String prefix){
        return messageService.infoTitled(env, "command.help.title", "command.poll.help",
                GuildConfig.formatPrefix(prefix));
    }
}
