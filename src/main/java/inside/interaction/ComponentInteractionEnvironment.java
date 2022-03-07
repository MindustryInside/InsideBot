package inside.interaction;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import inside.annotation.EnvironmentStyle;
import org.immutables.value.Value;

public interface ComponentInteractionEnvironment extends InteractionEnvironment {

    @Override
    ComponentInteractionEvent event();
}

@EnvironmentStyle
@Value.Immutable
interface ButtonInteractionEnvironmentDef extends ComponentInteractionEnvironment {

    @Override
    ButtonInteractionEvent event();
}

@EnvironmentStyle
@Value.Immutable
interface SelectMenuInteractionEnvironmentDef extends ComponentInteractionEnvironment {

    @Override
    SelectMenuInteractionEvent event();
}
