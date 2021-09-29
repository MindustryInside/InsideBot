package inside.command.admin;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.*;
import discord4j.core.spec.BanQuerySpec;
import discord4j.rest.util.Permission;
import inside.command.CommandCategory;
import inside.command.model.*;
import inside.util.*;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Predicate;

import static inside.service.MessageService.ok;
import static inside.util.ContextUtil.KEY_LOCALE;
import static reactor.function.TupleUtils.function;

@DiscordCommand(key = "softban", params = "command.admin.softban.params", description = "command.admin.softban.description",
        permissions = {Permission.SEND_MESSAGES, Permission.EMBED_LINKS, Permission.ADD_REACTIONS, Permission.BAN_MEMBERS},
        category = CommandCategory.admin)
public class SoftbanCommand extends AdminCommand{
    @Override
    public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
        Member author = env.member();

        Optional<Snowflake> targetId = interaction.getOption(0)
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asSnowflake);

        Optional<String> days = interaction.getOption(1)
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asString);

        if(days.isPresent() && days.filter(MessageUtil::canParseInt).isEmpty()){
            return messageService.err(env, "command.admin.softban.incorrect-days");
        }

        int deleteDays = days.map(Strings::parseInt).orElse(0);
        if(deleteDays > 7){
            DurationFormatter formatter = DurationFormat.wordBased(env.context().get(KEY_LOCALE));
            return messageService.err(env, "command.admin.softban.days-limit",
                    formatter.format(Duration.ofDays(7)));
        }

        String reason = interaction.getOption(2)
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asString)
                .map(String::trim)
                .orElse(null);

        Snowflake guildId = author.getGuildId();

        return Mono.justOrEmpty(targetId).flatMap(id -> env.message().getClient().getMemberById(guildId, id))
                .switchIfEmpty(messageService.err(env, "command.incorrect-name").then(Mono.never()))
                .filter(Predicate.not(User::isBot))
                .switchIfEmpty(messageService.err(env, "common.bot").then(Mono.never()))
                .filterWhen(target -> Mono.zip(adminService.isAdmin(target), adminService.isOwner(author))
                        .map(function((admin, owner) -> !(admin && !owner))))
                .switchIfEmpty(messageService.err(env, "command.admin.user-is-admin").then(Mono.never()))
                .flatMap(member -> member.getGuild().flatMap(guild -> guild.ban(member.getId(), BanQuerySpec.builder()
                                .reason(reason)
                                .deleteMessageDays(deleteDays)
                                .build()))
                        .then(member.getGuild().flatMap(guild -> guild.unban(member.getId()))))
                .and(env.message().addReaction(ok));
    }
}
