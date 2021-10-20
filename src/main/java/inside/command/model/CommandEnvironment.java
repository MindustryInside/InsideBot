package inside.command.model;

import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import inside.annotation.EnvironmentStyle;
import org.immutables.value.Value;
import reactor.util.context.ContextView;

@EnvironmentStyle
@Value.Immutable
interface CommandEnvironmentDef{

    GuildMessageChannel channel();

    ContextView context();

    Message message();

    Member member();
}
