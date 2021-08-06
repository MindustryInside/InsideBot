package inside.interaction;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.entity.channel.MessageChannel;
import org.immutables.builder.Builder;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import java.util.*;

public class InteractionCommandEnvironment{
    private final SlashCommandEvent event;
    private final ContextView context;
    private final List<ApplicationCommandInteractionOption> options;

    @Builder.Constructor
    protected InteractionCommandEnvironment(SlashCommandEvent event, ContextView context){
        this.event = event;
        this.context = context;
        options = new ArrayList<>();
        flattenOptions(event.getOptions(), options);
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

    public SlashCommandEvent event(){
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
