package inside.command.admin;

import discord4j.common.util.Snowflake;
import discord4j.rest.util.Permission;
import inside.command.CommandCategory;
import inside.command.model.*;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static inside.service.MessageService.ok;

@DiscordCommand(key = "unmute", params = "command.admin.unmute.params", description = "command.admin.unmute.description",
        permissions = {Permission.SEND_MESSAGES, Permission.EMBED_LINKS, Permission.ADD_REACTIONS, Permission.MANAGE_ROLES},
        category = CommandCategory.admin)
public class UnmuteCommand extends AdminCommand{
    @Override
    public Publisher<?> execute(CommandEnvironment env, CommandInteraction interaction){
        Optional<Snowflake> targetId = interaction.getOption("@user")
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asSnowflake);

        Snowflake guildId = env.member().getGuildId();

        return entityRetriever.getAdminConfigById(guildId)
                .switchIfEmpty(entityRetriever.createAdminConfig(guildId))
                .filter(adminConfig -> adminConfig.getMuteRoleID().isPresent())
                .switchIfEmpty(messageService.err(env, "command.disabled.mute").then(Mono.never()))
                .flatMap(ignored -> Mono.justOrEmpty(targetId))
                .flatMap(id -> env.message().getClient().getMemberById(guildId, id))
                .switchIfEmpty(messageService.err(env, "command.incorrect-name").then(Mono.never()))
                .filterWhen(adminService::isMuted)
                .switchIfEmpty(messageService.err(env, "audit.member.unmute.is-not-muted").then(Mono.never()))
                .flatMap(target -> adminService.unmute(target).and(env.message().addReaction(ok)));
    }
}
