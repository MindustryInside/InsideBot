package inside.event;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import inside.event.audit.AuditEventHandler;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class MiscMessageEventHandler extends AuditEventHandler{
    private static final String egg = "\uD83E\uDD5A";

    @Override
    public Publisher<?> onMessageCreate(MessageCreateEvent event){
        Message message = event.getMessage();
        String text = message.getContent().toLowerCase();

        if(text.contains("egg")){
            return message.addReaction(ReactionEmoji.unicode(egg));
        }

        return Mono.empty();
    }
}
