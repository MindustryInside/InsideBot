package inside.command.admin;

import discord4j.common.util.Snowflake;
import discord4j.core.object.audit.AuditLogEntry;
import discord4j.core.object.entity.*;
import discord4j.rest.util.Permission;
import inside.command.CommandCategory;
import inside.command.model.*;
import reactor.bool.BooleanUtils;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.function.Predicate;

import static inside.service.MessageService.ok;
import static reactor.function.TupleUtils.function;

@DiscordCommand(key = "mute", params = "command.admin.mute.params", description = "command.admin.mute.description",
        permissions = {Permission.SEND_MESSAGES, Permission.EMBED_LINKS, Permission.MANAGE_ROLES},
        category = CommandCategory.admin)
public class MuteCommand extends AdminCommand{
    @Override
    public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
        Member author = env.getAuthorAsMember();

        Optional<Snowflake> targetId = interaction.getOption("@user")
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asSnowflake);

        Snowflake guildId = author.getGuildId();

        ZonedDateTime delay = interaction.getOption("delay")
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asDateTime)
                .orElse(null);

        if(delay == null){
            return messageService.err(env, "message.error.invalid-time");
        }

        String reason = interaction.getOption("reason")
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asString)
                .map(String::trim)
                .orElse(null);

        return entityRetriever.getAdminConfigById(guildId)
                .switchIfEmpty(entityRetriever.createAdminConfig(guildId))
                .filter(adminConfig -> adminConfig.getMuteRoleID().isPresent())
                .switchIfEmpty(messageService.err(env, "command.disabled.mute").then(Mono.never()))
                .flatMap(ignored -> Mono.justOrEmpty(targetId))
                .flatMap(id -> env.getMessage().getClient().getMemberById(guildId, id))
                .switchIfEmpty(messageService.err(env, "command.incorrect-name").then(Mono.never()))
                .filter(Predicate.not(User::isBot))
                .switchIfEmpty(messageService.err(env, "common.bot").then(Mono.never()))
                .filterWhen(member -> BooleanUtils.not(adminService.isMuted(member)))
                .switchIfEmpty(messageService.err(env, "command.admin.mute.already-muted").then(Mono.never()))
                .filterWhen(member -> Mono.zip(adminService.isAdmin(member), adminService.isOwner(author))
                        .map(function((admin, owner) -> !(admin && !owner))))
                .switchIfEmpty(messageService.err(env, "command.admin.user-is-admin").then(Mono.never()))
                .flatMap(member -> {
                    if(author.equals(member)){
                        return messageService.err(env, "command.admin.mute.self-user");
                    }

                    if(reason != null && !reason.isBlank() && reason.length() >= AuditLogEntry.MAX_REASON_LENGTH){
                        return messageService.err(env, "common.string-limit", AuditLogEntry.MAX_REASON_LENGTH);
                    }

                    return adminService.mute(author, member, delay.toInstant(), reason)
                            .and(env.getMessage().addReaction(ok));
                });
    }
}
