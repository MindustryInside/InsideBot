package inside.interaction.settings;

import discord4j.core.object.entity.*;
import discord4j.rest.util.Permission;
import inside.interaction.InteractionCommandEnvironment;
import inside.interaction.common.GuildCommand;
import reactor.bool.BooleanUtils;
import reactor.core.publisher.Mono;

public abstract class SettingsCommand extends GuildCommand{
    @Override
    public Mono<Boolean> filter(InteractionCommandEnvironment env){
        Member member = env.event().getInteraction().getMember().orElse(null);
        if(member == null){
            return Mono.just(false);
        }

        Mono<Boolean> isOwner = member.getGuild().flatMap(Guild::getOwner)
                .map(member::equals);

        Mono<Boolean> isGuildManager = member.getHighestRole()
                .map(role -> role.getPermissions().contains(Permission.MANAGE_GUILD));

        Mono<Boolean> resp = BooleanUtils.or(isOwner, isGuildManager)
                .filterWhen(bool -> bool
                        ? Mono.just(true)
                        : messageService.text(env.event(), "command.owner-only").thenReturn(false));

        return BooleanUtils.and(super.filter(env), resp);
    }
}
