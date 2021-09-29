package inside.interaction;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.*;
import reactor.util.context.ContextView;

public abstract class InteractionEnvironment{

    public abstract ContextView context();

    public abstract InteractionCreateEvent event();

    public GatewayDiscordClient getClient(){
        return event().getClient();
    }
}
