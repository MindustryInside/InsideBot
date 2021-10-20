package inside.command.admin;

import discord4j.core.object.entity.*;
import discord4j.rest.util.Permission;
import inside.Settings;
import inside.audit.*;
import inside.command.CommandCategory;
import inside.command.model.*;
import inside.data.entity.GuildConfig;
import inside.util.*;
import inside.util.io.ReusableByteInputStream;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.BiConsumer;

import static inside.audit.Attribute.COUNT;
import static inside.audit.BaseAuditProvider.MESSAGE_TXT;
import static inside.service.MessageService.ok;
import static inside.util.ContextUtil.*;

@DiscordCommand(key = {"delete", "clear"}, params = "command.admin.delete.params", description = "command.admin.delete.description",
        permissions = {Permission.SEND_MESSAGES, Permission.EMBED_LINKS, Permission.ADD_REACTIONS,
                       Permission.MANAGE_MESSAGES, Permission.READ_MESSAGE_HISTORY},
        category = CommandCategory.admin)
public class DeleteCommand extends AdminCommand{
    @Autowired
    private Settings settings;

    @Autowired
    private AuditService auditService;

    @Override
    public Publisher<?> execute(CommandEnvironment env, CommandInteraction interaction){
        Member author = env.member();

        Optional<String> arg = interaction.getOption("count")
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asString);

        if(arg.filter(MessageUtil::canParseInt).isEmpty()){
            return messageService.err(env, "command.incorrect-number");
        }

        long number = arg.map(Strings::parseLong).orElse(0L);
        if(number > settings.getDiscord().getMaxClearedCount()){
            return messageService.err(env, "common.limit-number", settings.getDiscord().getMaxClearedCount());
        }

        StringBuffer result = new StringBuffer();
        Instant limit = Instant.now().minus(14, ChronoUnit.DAYS);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss")
                .withLocale(env.context().get(KEY_LOCALE))
                .withZone(env.context().get(KEY_TIMEZONE));

        BiConsumer<Message, Member> appendInfo = (message, member) -> {
            result.append("[").append(formatter.format(message.getTimestamp())).append("] ");
            if(DiscordUtil.isBot(member)){
                result.append("[BOT] ");
            }

            result.append(member.getUsername());
            member.getNickname().ifPresent(nickname -> result.append(" (").append(nickname).append(")"));
            result.append(" >");
            String content = MessageUtil.effectiveContent(message);
            if(!content.isBlank()){
                result.append(" ");
                if(content.contains("\n")){
                    result.append("\n");
                }
                result.append(content);
            }
            if(!message.getEmbeds().isEmpty()){
                int size = message.getEmbeds().size();
                result.append(" (... ").append(size).append(" embed").append(size > 1 ? "s" : "").append(")");
            }
            result.append("\n");
        };

        Mono<Void> history = env.channel()
                .getMessagesBefore(env.message().getId())
                        .take(number, true)
                        .sort(Comparator.comparing(Message::getId))
                        .filter(message -> message.getTimestamp().isAfter(limit))
                        .flatMap(message -> message.getAuthorAsMember()
                                .doOnNext(member -> appendInfo.accept(message, member))
                                .flatMap(ignored -> entityRetriever.deleteMessageInfoById(message.getId()))
                                .thenReturn(message))
                        .transform(messages -> number > 1 ? env.channel().bulkDeleteMessages(messages).then() :
                                messages.next().flatMap(Message::delete).then())
                .then();

        Mono<Void> log = auditService.newBuilder(author.getGuildId(), AuditActionType.MESSAGE_CLEAR)
                .withUser(author)
                .withChannel(env.channel())
                .withAttribute(COUNT, number)
                .withAttachment(MESSAGE_TXT, ReusableByteInputStream.ofString(result.toString()))
                .save();

        return history.then(log).then(env.message().addReaction(ok));
    }

    @Override
    public Publisher<?> help(CommandEnvironment env, String prefix){
        return messageService.infoTitled(env, "command.help.title", "command.admin.delete.help",
                GuildConfig.formatPrefix(prefix))
                .then();
    }
}
