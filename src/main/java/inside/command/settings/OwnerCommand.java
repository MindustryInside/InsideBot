package inside.command.settings;

import discord4j.core.object.entity.*;
import discord4j.rest.util.Permission;
import inside.command.Command;
import inside.command.model.CommandEnvironment;
import reactor.bool.BooleanUtils;
import reactor.core.publisher.Mono;

public abstract class OwnerCommand extends Command{
    @Override
    public Mono<Boolean> filter(CommandEnvironment env){
        Member member = env.getAuthorAsMember();

        Mono<Boolean> isOwner = member.getGuild().flatMap(Guild::getOwner)
                .map(member::equals);

        Mono<Boolean> isGuildManager = member.getHighestRole()
                .map(role -> role.getPermissions().contains(Permission.MANAGE_GUILD));

        return BooleanUtils.or(isOwner, isGuildManager);
    }
}
