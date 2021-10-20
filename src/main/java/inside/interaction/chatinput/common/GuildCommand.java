package inside.interaction.chatinput.common;

import inside.Settings;
import inside.interaction.*;
import inside.interaction.chatinput.BaseInteractionCommand;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

public abstract class GuildCommand extends BaseInteractionCommand{
    @Autowired
    protected Settings settings;

    @Override
    public Publisher<Boolean> filter(CommandEnvironment env){
        if(env.event().getInteraction().getMember().isEmpty()){
            return messageService.err(env, "command.interaction.only-guild").thenReturn(false);
        }
        return Mono.just(true);
    }
}
