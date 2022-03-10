package inside.interaction.chatinput;

import discord4j.core.object.entity.Member;
import discord4j.rest.util.Permission;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.PermissionCategory;
import inside.service.MessageService;
import reactor.core.publisher.Mono;

// Просто для маркирования
public abstract class InteractionGuildCommand extends InteractionCommand {

    public InteractionGuildCommand(MessageService messageService) {
        super(messageService);
    }

    @Override
    public Mono<Boolean> filter(ChatInputInteractionEnvironment env) {
        Member author = env.event().getInteraction().getMember().orElseThrow();

        return author.getBasePermissions()
                .map(set -> getPermissions().contains(PermissionCategory.EVERYONE) ||
                        getPermissions().contains(PermissionCategory.MODERATOR) && (set.contains(Permission.BAN_MEMBERS) ||
                                set.contains(Permission.KICK_MEMBERS) || set.contains(Permission.MODERATE_MEMBERS)) ||
                        getPermissions().contains(PermissionCategory.ADMIN) && set.contains(Permission.ADMINISTRATOR))
                .filter(Boolean::booleanValue)
                .switchIfEmpty(messageService.err(env, "commands.common.permission-denied").thenReturn(false));
    }
}
