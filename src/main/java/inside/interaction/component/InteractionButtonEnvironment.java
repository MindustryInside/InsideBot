package inside.interaction.component;

import discord4j.core.event.domain.interaction.*;
import inside.interaction.InteractionEnvironment;
import org.immutables.value.Value;

@Value.Immutable
public abstract class InteractionButtonEnvironment extends InteractionEnvironment{

    public static ImmutableInteractionButtonEnvironment.Builder builder(){
        return ImmutableInteractionButtonEnvironment.builder();
    }

    @Override
    public abstract ComponentInteractionEvent event();
}
