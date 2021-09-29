package inside.interaction.component.selectmenu;

import com.github.benmanes.caffeine.cache.*;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import inside.interaction.component.*;
import inside.service.MessageService;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Function;

@ComponentProvider("inside-search")
public class SearchSelectMenuListener implements SelectMenuListener{

    // messageId->function
    private final Cache<Snowflake, Function<InteractionSelectMenuEnvironment, Publisher<?>>> interactions = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(30))
            .build();

    private final MessageService messageService;

    public SearchSelectMenuListener(@Autowired MessageService messageService){
        this.messageService = messageService;
    }

    public Mono<Void> registerInteraction(Snowflake messageId, Function<InteractionSelectMenuEnvironment, Publisher<?>> func){
        return Mono.fromRunnable(() -> interactions.put(messageId, func));
    }

    @Override
    public Mono<Void> handle(InteractionSelectMenuEnvironment env){
        String[] parts = env.event().getCustomId().split("-"); // [ inside, search, 0 ]
        Snowflake authorId = Snowflake.of(parts[2]);

        Member target = env.event().getInteraction().getMember().orElse(null);
        if(target == null || !target.getId().equals(authorId)){
            return messageService.err(env, messageService.get(env.context(), "message.foreign-interaction"));
        }

        return Mono.justOrEmpty(interactions.getIfPresent(env.event().getMessageId()))
                .switchIfEmpty(messageService.err(env, "message.invalid-interaction").then(Mono.never()))
                .flatMap(func -> Mono.from(func.apply(env)))
                .then();
    }

    public Mono<Void> unregisterInteraction(Snowflake messageId){
        return Mono.fromRunnable(() -> interactions.invalidate(messageId));
    }
}
