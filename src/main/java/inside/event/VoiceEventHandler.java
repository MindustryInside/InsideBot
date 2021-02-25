package inside.event;

import discord4j.common.util.Snowflake;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import inside.data.service.EntityRetriever;
import inside.event.audit.AuditService;
import inside.util.DiscordUtil;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.context.Context;

import static inside.event.audit.AuditActionType.*;
import static inside.util.ContextUtil.*;

@Component
public class VoiceEventHandler extends ReactiveEventAdapter{
    @Autowired
    private AuditService auditService;

    @Autowired
    private EntityRetriever entityRetriever;

    @Override
    public Publisher<?> onVoiceStateUpdate(VoiceStateUpdateEvent event){
        Snowflake guildId = event.getCurrent().getGuildId();
        Context context = Context.of(KEY_GUILD_ID, guildId, KEY_LOCALE, entityRetriever.locale(guildId), KEY_TIMEZONE, entityRetriever.timeZone(guildId));
        if(!event.isJoinEvent() && !event.isLeaveEvent()){
            return Mono.empty();
        }

        return Mono.justOrEmpty(event.getOld())
                .defaultIfEmpty(event.getCurrent())
                .flatMap(state -> Mono.zip(state.getChannel(), state.getUser()))
                .filter(TupleUtils.predicate((channel, user) -> DiscordUtil.isNotBot(user)))
                .flatMap(TupleUtils.function((channel, user) -> auditService.log(guildId, event.isLeaveEvent() ? VOICE_LEAVE : VOICE_JOIN)
                        .withChannel(channel)
                        .withUser(user)
                        .save()))
                .contextWrite(context);
    }
}
