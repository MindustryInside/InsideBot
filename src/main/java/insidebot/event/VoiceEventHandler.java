package insidebot.event;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.json.MessageData;
import insidebot.audit.AuditEventHandler;
import insidebot.util.*;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static insidebot.audit.AuditEventType.*;

@Component
public class VoiceEventHandler extends AuditEventHandler{
    @Override
    public Publisher<?> onVoiceStateUpdate(VoiceStateUpdateEvent event){
        VoiceState state = event.getOld().orElse(null);
        Snowflake guildId = event.getCurrent().getGuildId();
        if(state != null){
            VoiceChannel channel = state.getChannel().block();
            User user = state.getUser().block();
            if(DiscordUtil.isBot(user) || channel == null) return Mono.empty();
            return log(guildId, embedBuilder -> {
                embedBuilder.setColor(voiceLeave.color);
                embedBuilder.setTitle(messageService.get("message.voice-leave"));
                embedBuilder.setDescription(messageService.format("message.voice-leave.text", user.getUsername(), channel.getName()));
                embedBuilder.setFooter(MessageUtil.zonedFormat(), null);
            });
        }else{
            VoiceState current = event.getCurrent();
            VoiceChannel channel = current.getChannel().block();
            User user = current.getUser().block();
            if(DiscordUtil.isBot(user) || channel == null) return Mono.empty();
            return log(guildId, embedBuilder -> {
                embedBuilder.setColor(voiceJoin.color);
                embedBuilder.setTitle(messageService.get("message.voice-join"));
                embedBuilder.setDescription(messageService.format("message.voice-join.text", user, channel.getName()));
                embedBuilder.setFooter(MessageUtil.zonedFormat(), null);
            });
        }
    }

    @Override
    public Mono<Void> log(Snowflake guildId, MessageCreateSpec message){
        MessageData data = discordService.getLogChannel(guildId)
                                         .flatMap(c -> c.getRestChannel().createMessage(message.asRequest()))
                                         .block();
        return Mono.justOrEmpty(data).flatMap(__ -> Mono.fromRunnable(() -> context.reset()));
    }
}
