package inside.command;

import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import inside.Configuration;
import inside.annotation.EnvironmentStyle;
import inside.service.InteractionService;
import inside.service.MessageService;
import org.immutables.value.Value;
import reactor.util.context.ContextView;

@EnvironmentStyle
@Value.Immutable
interface CommandEnvironmentDef {

    GuildMessageChannel channel();

    ContextView context();

    Message message();

    Member member();

    Configuration configuration();

    InteractionService interactionService();

    MessageService messageService();
}
