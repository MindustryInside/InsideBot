package inside.interaction;

import discord4j.core.event.domain.interaction.*;
import discord4j.core.object.command.*;
import org.immutables.value.Value;

import java.util.*;

@Value.Immutable
public abstract class InteractionCommandEnvironment extends InteractionEnvironment{

    public static ImmutableInteractionCommandEnvironment.Builder builder(){
        return ImmutableInteractionCommandEnvironment.builder();
    }

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
