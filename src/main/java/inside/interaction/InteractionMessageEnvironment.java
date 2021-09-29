package inside.interaction;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.MessageInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.retriever.EntityRetrievalStrategy;
import org.immutables.value.Value;
import reactor.core.publisher.Mono;

@Value.Immutable
public abstract class InteractionMessageEnvironment extends InteractionEnvironment{

    public static ImmutableInteractionMessageEnvironment.Builder builder(){
        return ImmutableInteractionMessageEnvironment.builder();
    }

    public Snowflake getTargetId(){
        return event().getTargetId();
    }

    public Mono<Message> getTargetMessage(){
        return event().getTargetMessage();
    }

    public Mono<Message> getTargetMessage(EntityRetrievalStrategy retrievalStrategy){
        return event().getTargetMessage(retrievalStrategy);
    }

    @Override
    public abstract MessageInteractionEvent event();
}
