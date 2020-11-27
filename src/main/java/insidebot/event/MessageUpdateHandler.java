package insidebot.event;

import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.object.Embed;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.spec.*;
import insidebot.common.services.DiscordService;
import insidebot.data.entity.MessageInfo;
import insidebot.data.services.MessageService;
import insidebot.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

import static insidebot.InsideBot.logChannelID;
import static insidebot.audit.AuditEventType.messageEdit;

@Component
public class MessageUpdateHandler extends AuditEventHandler<MessageUpdateEvent>{
    @Autowired
    private MessageService messageService;

    @Autowired
    private DiscordService discordService;

    @Override
    public Class<MessageUpdateEvent> type(){
        return MessageUpdateEvent.class;
    }

    @Override
    public Mono<Void> onEvent(MessageUpdateEvent event){
        Message message = event.getMessage().block();
        TextChannel c = message.getChannel().cast(TextChannel.class).block();
        User user = message.getAuthor().orElse(null);
        if(DiscordUtil.isBot(user) || c == null) return Mono.empty();
        if(!messageService.exists(event.getMessageId())) return Mono.empty();

        MessageInfo info = messageService.getById(event.getMessageId());

        String oldContent = info.content();
        String newContent = MessageUtil.effectiveContent(message);
        boolean under = newContent.length() >= Embed.Field.MAX_VALUE_LENGTH || oldContent.length() >= Embed.Field.MAX_VALUE_LENGTH;

        if(message.isPinned() || newContent.equals(oldContent)) return Mono.empty();

        Consumer<EmbedCreateSpec> e = embed -> {
            embed.setColor(messageEdit.color);
            embed.setAuthor(DiscordUtil.memberedName(user), null, user.getAvatarUrl());
            embed.setTitle(messageService.format("message.edit", c.getName()));
            embed.setDescription(messageService.format(event.getGuildId().isPresent() ? "message.edit.description" : "message.edit.nullable-guild",
                                                       event.getGuildId().get().asString(), /* Я не знаю как такое получить, но всё же обезопашусь */
                                                       event.getChannelId().asString(),
                                                       event.getMessageId().asString()));

            embed.addField(messageService.get("message.edit.old-content"),
                           MessageUtil.substringTo(oldContent, Embed.Field.MAX_VALUE_LENGTH), false);
            embed.addField(messageService.get("message.edit.new-content"),
                           MessageUtil.substringTo(newContent, Embed.Field.MAX_VALUE_LENGTH), true);

            embed.setFooter(MessageUtil.zonedFormat(), null);
        };

        if(under){
            stringInputStream.writeString(String.format("%s:\n%s\n\n%s:\n%s",
                                                        messageService.get("message.edit.old-content"), oldContent,
                                                        messageService.get("message.edit.new-content"), newContent));
        }

        log(e, under);

        info.content(newContent);
        messageService.save(info);
        return Mono.empty();
    }

    @Override
    public Mono<Void> log(MessageCreateSpec message){
        return discordService.getTextChannelById(logChannelID)
                             .flatMap(c -> c.getRestChannel().createMessage(message.asRequest()))
                             .then();
    }
}
