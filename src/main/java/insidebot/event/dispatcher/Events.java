package insidebot.event.dispatcher;

import discord4j.common.util.Snowflake;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.MessageData;
import insidebot.audit.AuditEventHandler;
import insidebot.event.dispatcher.EventType.*;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * Сборка всех ивентов
 */
public abstract class Events extends AuditEventHandler{

    public Publisher<?> onMessageClear(MessageClearEvent event){
        return Mono.empty();
    }

    public Publisher<?> onMemberUnmute(MemberUnmuteEvent event){
        return Mono.empty();
    }

    public Publisher<?> onMemberMute(MemberMuteEvent event){
        return Mono.empty();
    }

    @Override
    public Mono<Void> log(Snowflake guildId, MessageCreateSpec message){
        MessageData data = discordService.getLogChannel(guildId)
                                         .flatMap(c -> c.getRestChannel().createMessage(message.asRequest()))
                                         .block();
        return Mono.justOrEmpty(data).flatMap(__ -> Mono.fromRunnable(() -> context.reset()));
    }

    public final Publisher<?> hookOnEvent(BaseEvent event){
        if (event instanceof MessageClearEvent e) return onMessageClear(e);
        else if (event instanceof MemberUnmuteEvent e) return onMemberUnmute(e);
        else if (event instanceof MemberMuteEvent e) return onMemberMute(e);
        else return Mono.empty();
    }
}
