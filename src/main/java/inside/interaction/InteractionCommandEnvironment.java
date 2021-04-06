package inside.interaction;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.InteractionCreateEvent;
import reactor.util.context.Context;

import java.util.Objects;

public class InteractionCommandEnvironment{

    private final InteractionCreateEvent event;
    private final Context context;

    InteractionCommandEnvironment(Builder builder){
        Objects.requireNonNull(builder, "builder");
        this.event = builder.event;
        this.context = builder.context;
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

    public GatewayDiscordClient getClient(){
        return event().getClient();
    }

    public static class Builder{
        private InteractionCreateEvent event;
        private Context context;

        public Builder event(InteractionCreateEvent event){
            this.event = event;
            return this;
        }

        public Builder context(Context context){
            this.context = context;
            return this;
        }

        public InteractionCommandEnvironment build(){
            return new InteractionCommandEnvironment(this);
        }
    }
}
