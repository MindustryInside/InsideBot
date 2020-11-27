package insidebot.event;

import arc.struct.ObjectSet;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageDeleteEvent;
import discord4j.core.object.Embed.Field;
import discord4j.core.object.audit.*;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.*;
import insidebot.common.services.DiscordService;
import insidebot.data.entity.MessageInfo;
import insidebot.data.services.MessageService;
import insidebot.util.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

import static insidebot.InsideBot.logChannelID;
import static insidebot.audit.AuditEventType.messageDelete;

@Component
public class MessageDeleteHandler extends AuditEventHandler<MessageDeleteEvent>{
    @Autowired
    private MessageService messageService;

    @Autowired
    private DiscordService discordService;

    @Autowired
    private Logger log;

    protected static ObjectSet<Snowflake> buffer = new ObjectSet<>();

    @Override
    public Class<MessageDeleteEvent> type(){
        return MessageDeleteEvent.class;
    }

    @Override
    public Mono<Void> onEvent(MessageDeleteEvent event){
        Message m = event.getMessage().orElse(null);
        Guild guild = event.getGuild().block();
        if(guild != null && event.getChannelId().equals(logChannelID) && (m != null && !m.getEmbeds().isEmpty())){ /* =) */
            AuditLogEntry l = guild.getAuditLog().filter(a -> a.getActionType() == ActionType.MESSAGE_DELETE).blockFirst();
            return Mono.justOrEmpty(l).doOnNext(a -> {
                log.warn("User '{}' deleted log message", discordService.gateway().getUserById(a.getResponsibleUserId()).block().getUsername());
            }).then();
        }
        if(!messageService.exists(event.getMessageId())) return Mono.empty();
        if(buffer.contains(event.getMessageId())){
            return Mono.fromRunnable(() -> buffer.remove(event.getMessageId()));
        }

        MessageInfo info = messageService.getById(event.getMessageId());
        User user = info.user().asUser().block();
        TextChannel c =  event.getChannel().cast(TextChannel.class).block();
        String content = info.content();
        boolean under = content.length() >= Field.MAX_VALUE_LENGTH;

        if(c == null || DiscordUtil.isBot(user) || MessageUtil.isEmpty(content)) return Mono.empty();

        Consumer<EmbedCreateSpec> e = embed -> {
            embed.setColor(messageDelete.color);
            embed.setAuthor(DiscordUtil.memberedName(user), null, user.getAvatarUrl());
            embed.setTitle(messageService.format("message.delete", c.getName()));
            embed.setFooter(MessageUtil.zonedFormat(), null);
            embed.addField(messageService.get("message.delete.content"), MessageUtil.substringTo(content, Field.MAX_VALUE_LENGTH), true);
        };

        if(under){
            stringInputStream.writeString(String.format("%s:\n%s", messageService.get("message.delete.content"), content));
        }

        log(e, under);

        messageService.delete(info);
        return Mono.empty();
    }

    @Override
    public Mono<Void> log(MessageCreateSpec message){
        return discordService.getTextChannelById(logChannelID)
                             .flatMap(c -> c.getRestChannel().createMessage(message.asRequest()))
                             .then();
    }
}
