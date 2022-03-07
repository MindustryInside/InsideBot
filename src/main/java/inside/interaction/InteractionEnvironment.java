package inside.interaction;

import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import inside.Configuration;
import inside.service.InteractionService;
import reactor.util.context.ContextView;

public interface InteractionEnvironment {

    DeferrableInteractionEvent event();

    Configuration configuration();

    InteractionService interactionService();

    ContextView context();
}
