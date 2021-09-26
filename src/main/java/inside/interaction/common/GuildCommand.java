package inside.interaction.common;

import inside.Settings;
import inside.interaction.*;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

public abstract class GuildCommand extends BaseInteractionCommand{
    @Autowired
    protected Settings settings;

    @Override
    public Mono<Boolean> filter(InteractionCommandEnvironment env){
        if(env.event().getInteraction().getMember().isEmpty()){
            return messageService.err(env.event(), "command.interaction.only-guild").thenReturn(false);
        }
        return Mono.just(true);
    }
}
