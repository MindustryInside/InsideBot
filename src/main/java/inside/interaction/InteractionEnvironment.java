package inside.interaction;

import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import inside.Configuration;
import inside.service.InteractionService;

public interface InteractionEnvironment {

    DeferrableInteractionEvent event();

    Configuration configuration();

    InteractionService interactionService();
}
