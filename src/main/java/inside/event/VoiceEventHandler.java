package inside.event;

import arc.func.*;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.VoiceChannel;
import inside.event.audit.AuditEventHandler;
import inside.util.*;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
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
                             KEY_LOCALE, discordEntityRetrieveService.locale(guildId),
                             KEY_TIMEZONE, discordEntityRetrieveService.timeZone(guildId));

        Boolf<VoiceState> ignore = s -> s.isSelfDeaf() || s.isDeaf() || s.isMuted() || s.isSelfStreaming() ||
                                        s.isSelfVideoEnabled() || s.isSuppressed();
        if(state != null){
            if(ignore.get(state)) return Mono.empty();
            Mono<VoiceChannel> channel = state.getChannel();
            Mono<User> user = state.getUser();
            return Mono.zip(channel, user).filter(t -> DiscordUtil.isNotBot(t.getT2())).flatMap(t -> log(guildId, embed -> {
                embed.setColor(voiceLeave.color);
                embed.setTitle(messageService.get(context, "audit.voice.leave.title"));
                embed.setDescription(messageService.format(context, "audit.voice.leave.description", t.getT2().getUsername(), t.getT1().getName()));
                embed.setFooter(timestamp(), null);
            }));
        }else{
            VoiceState current = event.getCurrent();
            if(ignore.get(current)) return Mono.empty();
            Mono<VoiceChannel> channel = current.getChannel();
            Mono<User> user = current.getUser();
            return Mono.zip(channel, user).filter(t -> DiscordUtil.isNotBot(t.getT2())).flatMap(t -> log(guildId, embed -> {
                embed.setColor(voiceJoin.color);
                embed.setTitle(messageService.get(context, "audit.voice.join.title"));
                embed.setDescription(messageService.format(context, "audit.voice.join.description", t.getT2().getUsername(), t.getT1().getName()));
                embed.setFooter(timestamp(), null);
            }));
        }
    }
}
