package inside.command.admin;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.*;
import inside.command.CommandCategory;
import inside.command.model.*;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.function.Predicate;

@DiscordCommand(key = "unwarnall", params = "command.admin.unwarnall.params", description = "command.admin.unwarnall.description",
        category = CommandCategory.admin)
public class UnwarnAllCommand extends AdminCommand{
    @Override
    public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
        Member author = env.member();

        Optional<Snowflake> targetId = interaction.getOption("@user")
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asSnowflake);

        Snowflake guildId = author.getGuildId();

        return Mono.justOrEmpty(targetId).flatMap(id -> env.message().getClient().getMemberById(guildId, id))
                .switchIfEmpty(messageService.err(env, "command.incorrect-name").then(Mono.never()))
                .filter(Predicate.not(User::isBot))
                .switchIfEmpty(messageService.err(env, "common.bot").then(Mono.never()))
                .filterWhen(target -> adminService.isOwner(author).map(owner -> !target.equals(author) || owner))
                .switchIfEmpty(messageService.err(env, "command.admin.unwarnall.permission-denied").then(Mono.never())) // pluralized variant
                .flatMap(target -> messageService.text(env, "command.admin.unwarnall", target.getMention())
                        .then(adminService.unwarnAll(guildId, target.getId())));
    }
}
