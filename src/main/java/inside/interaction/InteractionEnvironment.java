package inside.interaction;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.*;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import inside.annotation.EnvironmentStyle;
import org.immutables.value.Value;
import reactor.util.context.ContextView;

import java.util.*;

public abstract class InteractionEnvironment{

    public abstract ContextView context();

    public abstract DeferrableInteractionEvent event();

    public GatewayDiscordClient getClient(){
        return event().getClient();
    }
}

@EnvironmentStyle
@Value.Immutable
abstract class CommandEnvironmentDef extends InteractionEnvironment{

    private static void flattenOptions(Iterable<? extends ApplicationCommandInteractionOption> options,
                                       Collection<? super ApplicationCommandInteractionOption> list){
        for(var opt : options){
            list.add(opt);
            flattenOptions(opt.getOptions(), list);
        }
    }

    @Value.Derived
    protected List<ApplicationCommandInteractionOption> getOptions(){
        List<ApplicationCommandInteractionOption> list = new ArrayList<>();
        flattenOptions(event().getOptions(), list);
        return list;
    }

    public Optional<ApplicationCommandInteractionOption> getOption(String name){
        return getOptions().stream()
                .filter(opt -> opt.getName().equals(name))
                .findFirst();
    }

    @Override
    public abstract ChatInputInteractionEvent event();
}

@EnvironmentStyle
@Value.Immutable
abstract class MessageEnvironmentDef extends InteractionEnvironment{

    @Override
    public abstract MessageInteractionEvent event();
}

@EnvironmentStyle
@Value.Immutable
abstract class UserEnvironmentDef extends InteractionEnvironment{

    @Override
    public abstract UserInteractionEvent event();
}

@EnvironmentStyle
@Value.Immutable
abstract class ButtonEnvironmentDef extends InteractionEnvironment{

    @Override
    public abstract ButtonInteractionEvent event();
}

@EnvironmentStyle
@Value.Immutable
abstract class SelectMenuEnvironmentDef extends InteractionEnvironment{

    @Override
    public abstract SelectMenuInteractionEvent event();
}
