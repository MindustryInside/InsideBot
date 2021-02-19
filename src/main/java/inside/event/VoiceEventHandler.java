package inside.event;

import arc.func.Boolf;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.VoiceState;
import inside.event.audit.AuditEventHandler;
import inside.util.DiscordUtil;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.context.Context;

import static inside.event.audit.AuditEventType.*;
import static inside.util.ContextUtil.*;

@Component
public class VoiceEventHandler extends AuditEventHandler{

    @Override
    public Publisher<?> onVoiceStateUpdate(VoiceStateUpdateEvent event){
        VoiceState state = event.getOld().orElse(null);
        Snowflake guildId = event.getCurrent().getGuildId();
        context = Context.of(KEY_GUILD_ID, guildId,
                             KEY_LOCALE, entityRetriever.locale(guildId),
                             KEY_TIMEZONE, entityRetriever.timeZone(guildId));

        Boolf<VoiceState> ignore = voiceState -> voiceState.isSelfDeaf() || voiceState.isDeaf() || voiceState.isMuted() || voiceState.isSelfStreaming() ||
                                                 voiceState.isSelfVideoEnabled() || voiceState.isSuppressed();
        if(state != null){
            if(ignore.get(state)) return Mono.empty();
            return Mono.zip(state.getChannel(), state.getUser())
                    .filter(TupleUtils.predicate((channel, user) -> DiscordUtil.isNotBot(user)))
                    .flatMap(TupleUtils.function((channel, user) -> log(guildId, embed -> embed.setColor(VOICE_LEAVE.color)
                            .setTitle(messageService.get(context, "audit.voice.leave.title"))
                            .setDescription(messageService.format(context, "audit.voice.leave.description", user.getUsername(), channel.getName()))
                            .setFooter(timestamp(), null))));
        }else{
            VoiceState current = event.getCurrent();
            if(ignore.get(current)) return Mono.empty();
            return Mono.zip(current.getChannel(), current.getUser())
                    .filter(TupleUtils.predicate((channel, user) -> DiscordUtil.isNotBot(user)))
                    .flatMap(TupleUtils.function((channel, user) -> log(guildId, embed -> embed.setColor(VOICE_JOIN.color)
                            .setTitle(messageService.get(context, "audit.voice.join.title"))
                            .setDescription(messageService.format(context, "audit.voice.join.description", user.getUsername(), channel.getName()))
                            .setFooter(timestamp(), null))));
        }
    }
}
