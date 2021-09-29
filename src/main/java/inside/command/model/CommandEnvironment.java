package inside.command.model;

import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import org.immutables.value.Value;
import reactor.util.context.ContextView;

@Value.Immutable
public abstract class CommandEnvironment{

    public static ImmutableCommandEnvironment.Builder builder(){
        return ImmutableCommandEnvironment.builder();
    }

    public abstract GuildMessageChannel channel();

    public abstract ContextView context();

    public abstract Message message();

    public abstract Member member();
}
