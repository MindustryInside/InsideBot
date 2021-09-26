package inside.interaction.component;

import com.github.benmanes.caffeine.cache.*;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.object.entity.Member;
import inside.service.MessageService;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Function;

@ComponentProvider("inside-search")
public class SearchSelectMenuListener implements SelectMenuListener{

    // messageId->function
    private final Cache<Snowflake, Function<SelectMenuInteractionEvent, Publisher<?>>> interactions = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(30))
            .build();

    private final MessageService messageService;

    public SearchSelectMenuListener(@Autowired MessageService messageService){
        this.messageService = messageService;
    }

    public Mono<Void> registerInteraction(Snowflake messageId, Function<SelectMenuInteractionEvent, Publisher<?>> func){
        return Mono.fromRunnable(() -> interactions.put(messageId, func));
    }

    @Override
    public Mono<Void> handle(SelectMenuInteractionEvent event){
        return Mono.deferContextual(ctx -> {
            String[] parts = event.getCustomId().split("-"); // [ inside, search, 0 ]
            Snowflake authorId = Snowflake.of(parts[2]);

            Member target = event.getInteraction().getMember().orElse(null);
            if(target == null || !target.getId().equals(authorId)){
                return messageService.err(event, messageService.get(ctx, "message.foreign-interaction"));
            }

            return Mono.justOrEmpty(interactions.getIfPresent(event.getMessageId()))
                    .switchIfEmpty(messageService.err(event, "message.invalid-interaction").then(Mono.never()))
                    .flatMap(func -> Mono.from(func.apply(event)))
                    .then();
        });
    }

    public Mono<Void> unregisterInteraction(Snowflake messageId){
        return Mono.fromRunnable(() -> interactions.invalidate(messageId));
    }
}
