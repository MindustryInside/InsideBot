package inside.interaction;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.command.*;
import discord4j.core.object.entity.channel.MessageChannel;
import org.immutables.builder.Builder;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import java.util.*;

public class InteractionCommandEnvironment{
    private final ApplicationCommandInteractionEvent event;
    private final ContextView context;
    private final List<ApplicationCommandInteractionOption> options;

    @Builder.Constructor
    protected InteractionCommandEnvironment(ApplicationCommandInteractionEvent event, ContextView context){
        this.event = event;
        this.context = context;
        options = new ArrayList<>();
        ApplicationCommandInteraction commandInteraction = event.getInteraction().getCommandInteraction()
                .orElseThrow(IllegalStateException::new);

        flattenOptions(commandInteraction.getOptions(), options);
    }

    private static void flattenOptions(Iterable<? extends ApplicationCommandInteractionOption> options,
                                       Collection<? super ApplicationCommandInteractionOption> list){
        for(var opt : options){
            list.add(opt);
            flattenOptions(opt.getOptions(), list);
        }
    }

    public static InteractionCommandEnvironmentBuilder builder(){
        return new InteractionCommandEnvironmentBuilder();
    }

    public ApplicationCommandInteractionEvent event(){
        return event;
    }

    public ContextView context(){
        return context;
    }

    public Mono<MessageChannel> getReplyChannel(){
        return event.getInteraction().getChannel();
    }

    public GatewayDiscordClient getClient(){
        return event().getClient();
    }

    public Optional<ApplicationCommandInteractionOption> getOption(String name){
        return options.stream()
                .filter(opt -> opt.getName().equals(name))
                .findFirst();
    }
}
