package inside.interaction.chatinput.settings;

import discord4j.core.object.entity.*;
import discord4j.rest.util.Permission;
import inside.interaction.CommandEnvironment;
import inside.interaction.chatinput.common.GuildCommand;
import org.reactivestreams.Publisher;
import reactor.bool.BooleanUtils;
import reactor.core.publisher.Mono;

public abstract class SettingsCommand extends GuildCommand{
    @Override
    public Publisher<Boolean> filter(CommandEnvironment env){
        Member member = env.event().getInteraction().getMember().orElse(null);
        if(member == null){
            return Mono.just(false);
        }

        Mono<Boolean> isOwner = member.getGuild().flatMap(Guild::getOwner)
                .map(member::equals);

        Mono<Boolean> isGuildManager = member.getHighestRole()
                .map(role -> role.getPermissions().contains(Permission.MANAGE_GUILD));

        Mono<Boolean> resp = BooleanUtils.or(isOwner, isGuildManager)
                .filterWhen(bool -> bool ? Mono.just(true) : messageService.err(env, "command.owner-only").thenReturn(false));

        return BooleanUtils.and(Mono.from(super.filter(env)), resp);
    }
}
