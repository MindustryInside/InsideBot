package insidebot.event;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.*;
import insidebot.data.entity.*;
import insidebot.data.services.UserService;
import insidebot.util.MessageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Calendar;

import static insidebot.InsideBot.commands;

@Component
public class MessageCreateHandler implements EventHandled<MessageCreateEvent>{
    @Autowired
    private UserService userService;

    @Override
    public Class<MessageCreateEvent> type(){
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<Void> onEvent(MessageCreateEvent event){
        User user = event.getMessage().getAuthor().orElse(null);
        if(user == null || user.isBot()) return Mono.empty();
        Message message = event.getMessage();
        MessageInfo info = new MessageInfo();
        UserInfo userInfo = userService.getOr(user.getId(), UserInfo::new);

        userInfo.name(user.getUsername());
        userInfo.userId(user.getId());
        userInfo.lastSentMessage(Calendar.getInstance());
        userInfo.addToSeq();

        info.user(userInfo);
        info.id(message.getId());
        info.guildId(message.getGuildId().orElseThrow(RuntimeException::new)); // Не надо мне тут
        info.channelId(message.getChannelId());
        info.timestamp(Calendar.getInstance());

        info.content(MessageUtil.effectiveContent(message));
        userInfo.messageInfo().add(info);

        commands.handle(event);
        userService.save(userInfo);
        return Mono.empty();
    }
}
