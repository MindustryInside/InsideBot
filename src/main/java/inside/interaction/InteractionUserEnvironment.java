package inside.interaction;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.UserInteractionEvent;
import discord4j.core.object.entity.User;
import discord4j.core.retriever.EntityRetrievalStrategy;
import org.immutables.value.Value;
import reactor.core.publisher.Mono;

@Value.Immutable
public abstract class InteractionUserEnvironment extends InteractionEnvironment{

    public static ImmutableInteractionUserEnvironment.Builder builder(){
        return ImmutableInteractionUserEnvironment.builder();
    }

    public Snowflake getTargetId(){
        return event().getTargetId();
    }

    public Mono<User> getTargetUser(){
        return event().getTargetUser();
    }

    public Mono<User> getTargetUser(EntityRetrievalStrategy retrievalStrategy){
        return event().getTargetUser(retrievalStrategy);
    }

    @Override
    public abstract UserInteractionEvent event();
}
