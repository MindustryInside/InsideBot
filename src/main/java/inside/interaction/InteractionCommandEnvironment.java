package inside.interaction;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.InteractionCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.Objects;
import java.util.function.Supplier;

public class InteractionCommandEnvironment{

    private final InteractionCreateEvent event;
    private final Context context;
    private final Supplier<Mono<MessageChannel>> replyChannel;

    InteractionCommandEnvironment(Builder builder){
        Objects.requireNonNull(builder, "builder");
        this.event = builder.event;
        this.context = builder.context;
        this.replyChannel = builder.replyChannel;
    }

    public static Builder builder(){
        return new Builder();
    }

    public InteractionCreateEvent event(){
        return event;
    }

    public Context context(){
        return context;
    }

    public Mono<MessageChannel> getReplyChannel(){
        return getClient().getChannelById(event.getInteraction().getChannelId())
                .cast(MessageChannel.class);
    }

    public GatewayDiscordClient getClient(){
        return event().getClient();
    }

    public static class Builder{
        private InteractionCreateEvent event;
        private Context context;
        private Supplier<Mono<MessageChannel>> replyChannel;

        public Builder event(InteractionCreateEvent event){
            this.event = event;
            return this;
        }

        public Builder context(Context context){
            this.context = context;
            return this;
        }

        public Builder replyChannel(Supplier<Mono<MessageChannel>> replyChannel){
            this.replyChannel = replyChannel;
            return this;
        }

        public InteractionCommandEnvironment build(){
            return new InteractionCommandEnvironment(this);
        }
    }
}
