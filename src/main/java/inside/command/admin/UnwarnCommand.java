package inside.command.admin;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.*;
import inside.command.CommandCategory;
import inside.command.model.*;
import inside.util.*;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.function.Predicate;

@DiscordCommand(key = "unwarn", params = "command.admin.unwarn.params", description = "command.admin.unwarn.description",
        category = CommandCategory.admin)
public class UnwarnCommand extends AdminCommand{
    @Override
    public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
        Member author = env.member();

        Optional<Snowflake> targetId = interaction.getOption("@user")
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asSnowflake);

        Snowflake guildId = author.getGuildId();

        Optional<String> index = interaction.getOption("index")
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asString);

        if(index.filter(MessageUtil::canParseInt).isEmpty()){
            return messageService.err(env, "command.incorrect-number");
        }

        return Mono.justOrEmpty(targetId).flatMap(id -> env.message().getClient().getMemberById(guildId, id))
                .switchIfEmpty(messageService.err(env, "command.incorrect-name").then(Mono.never()))
                .filter(Predicate.not(User::isBot))
                .switchIfEmpty(messageService.err(env, "common.bot").then(Mono.never()))
                .filterWhen(target -> adminService.isOwner(author).map(owner -> !target.equals(author) || owner))
                .switchIfEmpty(messageService.err(env, "command.admin.unwarn.permission-denied").then(Mono.never()))
                .flatMap(target -> adminService.warnings(target).count().flatMap(count -> {
                    int warn = index.map(Strings::parseInt).orElse(1);
                    if(count == 0){
                        return messageService.text(env, "command.admin.warnings.empty");
                    }

                    if(warn > count){
                        return messageService.err(env, "command.incorrect-number");
                    }

                    return messageService.text(env, "command.admin.unwarn", target.getUsername(), warn)
                            .and(adminService.unwarn(target, warn - 1));
                }))
                .then();
    }
}
