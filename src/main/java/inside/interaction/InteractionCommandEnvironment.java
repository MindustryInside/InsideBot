package inside.interaction;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.InteractionCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import java.util.Objects;
import java.util.function.Supplier;

public class InteractionCommandEnvironment{
    private final InteractionCreateEvent event;
    private final ContextView context;
    private final Supplier<Mono<MessageChannel>> replyChannel;

    InteractionCommandEnvironment(InteractionCreateEvent event, ContextView context, Supplier<Mono<MessageChannel>> replyChannel){
        this.event = Objects.requireNonNull(event, "event");
        this.context = Objects.requireNonNull(context, "context");
        this.replyChannel = Objects.requireNonNull(replyChannel, "replyChannel");
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
        return replyChannel.get();
    }

    public GatewayDiscordClient getClient(){
        return event().getClient();
    }

    public static class Builder{
        private InteractionCreateEvent event;
        private ContextView context;
        private Supplier<Mono<MessageChannel>> replyChannel;

        public Builder event(InteractionCreateEvent event){
            this.event = event;
            return this;
        }

        public Builder context(ContextView context){
            this.context = context;
            return this;
        }

        public Builder replyChannel(Supplier<Mono<MessageChannel>> replyChannel){
            this.replyChannel = replyChannel;
            return this;
        }

        public InteractionCommandEnvironment build(){
            if(replyChannel == null){
                this.replyChannel = () -> event.getClient().getChannelById(event.getInteraction().getChannelId())
                        .cast(MessageChannel.class);
            }
            return new InteractionCommandEnvironment(event, context, replyChannel);
        }
    }
}
