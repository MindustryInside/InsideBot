package inside.interaction;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import inside.annotation.EnvironmentStyle;
import org.immutables.value.Value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@EnvironmentStyle
@Value.Immutable
abstract class ChatInputInteractionEnvironmentDef implements InteractionEnvironment {

    private static void flattenOptions(Iterable<? extends ApplicationCommandInteractionOption> options,
                                       Collection<? super ApplicationCommandInteractionOption> list) {
        for (var opt : options) {
            list.add(opt);
            flattenOptions(opt.getOptions(), list);
        }
    }

    @Override
    public abstract ChatInputInteractionEvent event();

    @Value.Derived
    protected List<ApplicationCommandInteractionOption> getOptions() {
        List<ApplicationCommandInteractionOption> list = new ArrayList<>();
        flattenOptions(event().getOptions(), list);
        return list;
    }

    public Optional<ApplicationCommandInteractionOption> getOption(String name) {
        return getOptions().stream()
                .filter(opt -> opt.getName().equals(name))
                .findFirst();
    }
}
