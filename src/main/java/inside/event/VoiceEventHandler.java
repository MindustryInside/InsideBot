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
        if(!event.isJoinEvent() && !event.isLeaveEvent()){
            return Mono.empty();
        }
        Mono<Context> initContext = entityRetriever.getGuildConfigById(guildId)
                .switchIfEmpty(entityRetriever.createGuildConfig(guildId))
                .map(guildConfig -> Context.of(KEY_LOCALE, guildConfig.locale(),
                        KEY_TIMEZONE, guildConfig.timeZone()));

        return initContext.flatMap(context -> Mono.justOrEmpty(event.getOld())
                .defaultIfEmpty(event.getCurrent())
                .flatMap(state -> Mono.zip(state.getChannel(), state.getUser()))
                .filter(TupleUtils.predicate((channel, user) -> DiscordUtil.isNotBot(user)))
                .flatMap(TupleUtils.function((channel, user) -> auditService.log(guildId, event.isLeaveEvent() ? VOICE_LEAVE : VOICE_JOIN)
                        .withChannel(channel)
                        .withUser(user)
                        .save()))
                .contextWrite(context));
    }
}
