package inside.interaction.chatinput;

import discord4j.core.object.entity.Member;
import discord4j.rest.util.Permission;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.PermissionCategory;
import inside.service.MessageService;
import reactor.core.publisher.Mono;

public abstract class InteractionGuildCommand extends InteractionCommand {

    public InteractionGuildCommand(MessageService messageService) {
        super(messageService);
    }

    @Override
    public Mono<Boolean> filter(ChatInputInteractionEnvironment env) {
        Member author = env.event().getInteraction().getMember().orElseThrow();

        return author.getBasePermissions()
                .map(set -> info.permissions().contains(PermissionCategory.EVERYONE) ||
                        info.permissions().contains(PermissionCategory.MODERATOR) && (set.contains(Permission.BAN_MEMBERS) ||
                                set.contains(Permission.KICK_MEMBERS) || set.contains(Permission.MODERATE_MEMBERS)) ||
                        info.permissions().contains(PermissionCategory.ADMIN) && set.contains(Permission.ADMINISTRATOR))
                .filter(Boolean::booleanValue)
                .switchIfEmpty(messageService.err(env, "common.not-enough-permissions").thenReturn(false));
    }
}
