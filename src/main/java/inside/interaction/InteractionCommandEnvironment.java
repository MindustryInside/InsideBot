package inside.interaction;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.InteractionCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import java.util.Objects;

public class InteractionCommandEnvironment{
    private final InteractionCreateEvent event;
    private final ContextView context;

    InteractionCommandEnvironment(InteractionCreateEvent event, ContextView context){
        this.event = Objects.requireNonNull(event, "event");
        this.context = Objects.requireNonNull(context, "context");
    }

    public static Builder builder(){
        return new Builder();
    }

    public InteractionCreateEvent event(){
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

    public static class Builder{
        private InteractionCreateEvent event;
        private ContextView context;

        public Builder event(InteractionCreateEvent event){
            this.event = event;
            return this;
        }

        public Builder context(ContextView context){
            this.context = context;
            return this;
        }

        public InteractionCommandEnvironment build(){
            return new InteractionCommandEnvironment(event, context);
        }
    }
}
