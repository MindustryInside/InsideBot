package inside.interaction.component;

import discord4j.core.event.domain.interaction.*;
import inside.interaction.InteractionEnvironment;
import org.immutables.value.Value;

@Value.Immutable
public abstract class InteractionSelectMenuEnvironment extends InteractionEnvironment{

    public static ImmutableInteractionSelectMenuEnvironment.Builder builder(){
        return ImmutableInteractionSelectMenuEnvironment.builder();
    }

    @Override
    public abstract SelectMenuInteractionEvent event();
}
