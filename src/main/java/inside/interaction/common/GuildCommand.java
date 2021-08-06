package inside.interaction.common;

import inside.Settings;
import inside.interaction.*;
import inside.util.ContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

public abstract class GuildCommand extends InteractionCommand{
    @Autowired
    protected Settings settings;

    @Override
    public Mono<Boolean> filter(InteractionCommandEnvironment env){
        if(env.event().getInteraction().getMember().isEmpty()){
            return messageService.text(env.event(), "command.interaction.only-guild")
                    .contextWrite(ctx -> ctx.put(ContextUtil.KEY_EPHEMERAL, true)).thenReturn(false);
        }
        return Mono.just(true);
    }
}
