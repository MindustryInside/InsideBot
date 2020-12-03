package insidebot.event;

import arc.func.*;
import arc.util.Log;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.VoiceChannel;
import insidebot.event.audit.AuditEventHandler;
import insidebot.util.*;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static insidebot.event.audit.AuditEventType.*;

@Component
public class VoiceEventHandler extends AuditEventHandler{
    @Override
    public Publisher<?> onVoiceStateUpdate(VoiceStateUpdateEvent event){
        VoiceState state = event.getOld().orElse(null);
        Snowflake guildId = event.getCurrent().getGuildId();
        context.init(guildId);
        Boolf<VoiceState> ignore = s -> s.isSelfDeaf() || s.isDeaf() || s.isMuted() || s.isSelfStreaming() ||
                                        s.isSelfVideoEnabled() || s.isSuppressed();
        if(state != null){
            if(ignore.get(state)) return Mono.empty();
            VoiceChannel channel = state.getChannel().block();
            User user = state.getUser().block();
            if(DiscordUtil.isBot(user) || channel == null) return Mono.empty();
            return log(guildId, embed -> {
                embed.setColor(voiceLeave.color);
                embed.setTitle(messageService.get("message.voice-leave"));
                embed.setDescription(messageService.format("message.voice-leave.text", user.getUsername(), channel.getName()));
                embed.setFooter(MessageUtil.zonedFormat(), null);
            });
        }else{
            VoiceState current = event.getCurrent();
            if(ignore.get(current)) return Mono.empty();
            VoiceChannel channel = current.getChannel().block();
            User user = current.getUser().block();
            if(DiscordUtil.isBot(user) || channel == null) return Mono.empty();
            return log(guildId, embed -> {
                embed.setColor(voiceJoin.color);
                embed.setTitle(messageService.get("message.voice-join"));
                embed.setDescription(messageService.format("message.voice-join.text", user.getUsername(), channel.getName()));
                embed.setFooter(MessageUtil.zonedFormat(), null);
            });
        }
    }
}
