package inside.command.admin;

import discord4j.common.util.Snowflake;
import discord4j.core.object.audit.AuditLogEntry;
import discord4j.core.object.entity.*;
import discord4j.core.spec.BanQuerySpec;
import discord4j.rest.util.Permission;
import inside.command.CommandCategory;
import inside.command.model.*;
import inside.data.entity.AdminConfig;
import inside.util.Strings;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Predicate;

import static reactor.function.TupleUtils.function;

@DiscordCommand(key = "warn", params = "command.admin.warn.params", description = "command.admin.warn.description",
        permissions = {Permission.SEND_MESSAGES, Permission.EMBED_LINKS, Permission.BAN_MEMBERS},
        category = CommandCategory.admin)
public class WarnCommand extends AdminCommand{
    @Override
    public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
        Member author = env.getAuthorAsMember();

        Optional<Snowflake> targetId = interaction.getOption(0)
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asSnowflake);

        String reason = interaction.getOption(1)
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asString)
                .map(String::trim)
                .orElse(null);

        Snowflake guildId = author.getGuildId();

        return Mono.justOrEmpty(targetId).flatMap(id -> env.getClient().getMemberById(guildId, id))
                .switchIfEmpty(messageService.err(env, "command.incorrect-name").then(Mono.never()))
                .filter(Predicate.not(User::isBot))
                .switchIfEmpty(messageService.err(env, "common.bot").then(Mono.never()))
                .filterWhen(target -> Mono.zip(adminService.isAdmin(target), adminService.isOwner(author))
                        .map(function((admin, owner) -> !(admin && !owner))))
                .switchIfEmpty(messageService.err(env, "command.admin.user-is-admin").then(Mono.never()))
                .flatMap(member -> {
                    if(author.equals(member)){
                        return messageService.err(env, "command.admin.warn.self-user");
                    }

                    if(!Strings.isEmpty(reason) && reason.length() >= AuditLogEntry.MAX_REASON_LENGTH){
                        return messageService.err(env, "common.string-limit", AuditLogEntry.MAX_REASON_LENGTH);
                    }

                    Mono<Void> warnings = Mono.defer(() -> adminService.warnings(member).count()).flatMap(count -> {
                        Mono<Void> message = messageService.text(env, "command.admin.warn", member.getUsername(), count);

                        Mono<AdminConfig> config = entityRetriever.getAdminConfigById(guildId)
                                .switchIfEmpty(entityRetriever.createAdminConfig(guildId));

                        String autoReason = messageService.format(env.context(), "message.admin.auto-reason", count);

                        Mono<Void> thresholdCheck = config.filter(adminConfig -> count >= adminConfig.getMaxWarnCount())
                                .flatMap(adminConfig -> switch(adminConfig.getThresholdAction()){
                                    case ban -> author.getGuild().flatMap(guild ->
                                            guild.ban(member.getId(), BanQuerySpec.builder()
                                                    .deleteMessageDays(0)
                                                    .reason(autoReason)
                                                    .build()));
                                    case kick -> author.kick();
                                    case mute -> author.getGuild().flatMap(Guild::getOwner)
                                            .flatMap(owner -> adminService.mute(owner, author,
                                                    Instant.now().plus(adminConfig.getMuteBaseDelay()), autoReason));
                                    default -> Mono.empty();
                                });

                        return message.then(thresholdCheck);
                    });

                    return adminService.warn(author, member, reason).then(warnings);
                });
    }
}
