package inside.command.admin;

import discord4j.common.util.*;
import discord4j.core.object.Embed;
import discord4j.core.object.component.*;
import discord4j.core.object.entity.*;
import discord4j.core.spec.*;
import inside.Settings;
import inside.command.CommandCategory;
import inside.command.model.*;
import inside.util.Mathf;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.Optional;
import java.util.function.Predicate;

import static reactor.function.TupleUtils.function;

@DiscordCommand(key = {"warnings", "warns"}, params = "command.admin.warnings.params", description = "command.admin.warnings.description",
        category = CommandCategory.admin)
public class WarningsCommand extends AdminCommand{
    public static final int PER_PAGE = 9;

    @Autowired
    private Settings settings;

    @Override
    public Publisher<?> execute(CommandEnvironment env, CommandInteraction interaction){
        Optional<Snowflake> targetId = interaction.getOption(0)
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asSnowflake);

        Mono<Member> referencedUser = Mono.justOrEmpty(env.message().getMessageReference())
                .flatMap(ref -> Mono.justOrEmpty(ref.getMessageId()).flatMap(messageId ->
                        env.message().getClient().getMessageById(ref.getChannelId(), messageId)))
                .flatMap(Message::getAuthorAsMember);

        Snowflake guildId = env.member().getGuildId();

        Snowflake authorId = env.member().getId();

        return Mono.justOrEmpty(targetId)
                .flatMap(userId -> env.message().getClient().getMemberById(guildId, userId))
                .switchIfEmpty(referencedUser)
                .switchIfEmpty(messageService.err(env, "command.incorrect-name").then(Mono.never()))
                .filter(Predicate.not(User::isBot))
                .switchIfEmpty(messageService.err(env, "common.bot").then(Mono.never()))
                .zipWhen(member -> adminService.warnings(member)
                        .switchIfEmpty(messageService.text(env, "command.admin.warnings.empty").then(Mono.never()))
                        .take(PER_PAGE, true).index()
                        .map(function((idx, warn) ->
                                EmbedCreateFields.Field.of(String.format("%2s. %s", idx + 1,
                                                TimestampFormat.LONG_DATE_TIME.format(warn.getTimestamp())), String.format("%s%n%s",
                                                messageService.format(env.context(), "common.admin", warn.getAdmin().getEffectiveName()),
                                                messageService.format(env.context(), "common.reason", warn.getReason()
                                                        .orElse(messageService.get(env.context(), "common.not-defined")))),
                                        true)))
                        .collectList())
                .zipWhen(tuple -> adminService.warnings(tuple.getT1()).count(),
                        (tuple, count) -> Tuples.of(tuple.getT1(), tuple.getT2(), count))
                .flatMap(function((target, fields, count) -> env.channel()
                        .createMessage(MessageCreateSpec.builder()
                                .addEmbed(EmbedCreateSpec.builder()
                                        .title(messageService.format(env.context(), "command.admin.warnings.title",
                                                target.getDisplayName()))
                                        .fields(fields)
                                        .color(settings.getDefaults().getNormalColor())
                                        .footer(messageService.format(env.context(), "command.admin.warnings.page",
                                                1, Mathf.ceilPositive(count / (float)PER_PAGE)), null)
                                        .build())
                                .addComponent(ActionRow.of(
                                        Button.primary("inside-warnings-" + authorId.asString() +
                                                                "-" + target.getId().asString() + "-prev-0",
                                                        messageService.get(env.context(), "common.prev-page"))
                                                .disabled(),
                                        Button.primary("inside-warnings-" + authorId.asString() +
                                                                "-" + target.getId().asString() + "-next-1",
                                                        messageService.get(env.context(), "common.next-page"))
                                                .disabled(count <= PER_PAGE)))
                                .build())))
                .then();
    }
}
