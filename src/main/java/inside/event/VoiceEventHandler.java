package inside.event;

import arc.func.Boolf;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.VoiceState;
import inside.event.audit.*;
import inside.util.DiscordUtil;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.context.Context;

import static inside.event.audit.AuditEventType.*;
import static inside.util.ContextUtil.*;

@Component
public class VoiceEventHandler extends AuditEventHandler{

    @Autowired
    private AuditService auditService;

    @Override
    public Publisher<?> onVoiceStateUpdate(VoiceStateUpdateEvent event){
        Snowflake guildId = event.getCurrent().getGuildId();
        context = Context.of(KEY_GUILD_ID, guildId,
                             KEY_LOCALE, entityRetriever.locale(guildId),
                             KEY_TIMEZONE, entityRetriever.timeZone(guildId));

        Boolf<VoiceState> ignore = voiceState -> !(voiceState.isSelfDeaf() || voiceState.isDeaf() || voiceState.isMuted() || voiceState.isSelfStreaming() ||
                                                 voiceState.isSelfVideoEnabled() || voiceState.isSuppressed());

        return Mono.justOrEmpty(event.getOld()).filter(ignore::get)
                .switchIfEmpty(Mono.justOrEmpty(event.getCurrent())
                        .filter(ignore::get)
                        .flatMap(state -> Mono.zip(state.getChannel(), state.getUser())
                        .filter(TupleUtils.predicate((channel, user) -> DiscordUtil.isNotBot(user)))
                        .flatMap(TupleUtils.function((channel, user) -> auditService.log(guildId, VOICE_JOIN)
                                .withChannel(channel)
                                .withUser(user)
                                .save()))
                ).then(Mono.empty()))
                .flatMap(state -> Mono.zip(state.getChannel(), state.getUser())
                        .filter(TupleUtils.predicate((channel, user) -> DiscordUtil.isNotBot(user)))
                        .flatMap(TupleUtils.function((channel, user) -> auditService.log(guildId, VOICE_LEAVE)
                                .withChannel(channel)
                                .withUser(user)
                                .save())))
                .contextWrite(context);
    }
}
