package inside.interaction;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import inside.annotation.EnvironmentStyle;
import org.immutables.value.Value;

@EnvironmentStyle
@Value.Immutable
interface ComponentInteractionEnvironmentDef<T extends ComponentInteractionEvent> extends InteractionEnvironment {

    @Override
    T event();
}

@EnvironmentStyle
@Value.Immutable
interface ButtonInteractionEnvironmentDef extends ComponentInteractionEnvironmentDef<ButtonInteractionEvent> {}

@EnvironmentStyle
@Value.Immutable
interface SelectMenuInteractionEnvironmentDef extends ComponentInteractionEnvironmentDef<SelectMenuInteractionEvent> {}
