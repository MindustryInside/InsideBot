package insidebot.event;

import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.spec.MessageCreateSpec;
import insidebot.audit.AuditEventHandler;
import insidebot.common.services.DiscordService;
import insidebot.data.service.MessageService;
import insidebot.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.*;

import static insidebot.InsideBot.logChannelID;
import static insidebot.audit.AuditEventType.*;

@Component
public class VoiceStateUpdateHandler extends AuditEventHandler<VoiceStateUpdateEvent>{
    @Autowired
    private MessageService messageService;

    @Autowired
    private DiscordService discordService;

    @Override
    public Class<VoiceStateUpdateEvent> type(){
        return VoiceStateUpdateEvent.class;
    }

    @Override
    public Mono<Void> onEvent(VoiceStateUpdateEvent event){
        VoiceState state = event.getOld().orElse(null);
        if(state != null){
            VoiceChannel channel = state.getChannel().block();
            User user = state.getUser().block();
            if(DiscordUtil.isBot(user) || channel == null) return Mono.empty();
            return log(embedBuilder -> {
                embedBuilder.setColor(voiceLeave.color);
                embedBuilder.setTitle(messageService.get("message.voice-leave"));
                embedBuilder.setDescription(messageService.format("message.voice-leave.text", user.getUsername(), channel.getName()));
                embedBuilder.setFooter(MessageUtil.zonedFormat(), null);
            });
        }else{
            VoiceChannel channel = event.getCurrent().getChannel().block();
            User user = event.getCurrent().getUser().block();
            if(DiscordUtil.isBot(user) || channel == null) return Mono.empty();
            return log(embedBuilder -> {
                embedBuilder.setColor(voiceJoin.color);
                embedBuilder.setTitle(messageService.get("message.voice-join"));
                embedBuilder.setDescription(messageService.format("message.voice-join.text", user, channel.getName()));
                embedBuilder.setFooter(MessageUtil.zonedFormat(), null);
            });
        }
    }

    @Override
    public Mono<Void> log(MessageCreateSpec message){
        return discordService.getTextChannelById(logChannelID)
                             .flatMap(c -> c.getRestChannel().createMessage(message.asRequest()))
                             .then();
    }
}
