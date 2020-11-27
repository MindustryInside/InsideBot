package insidebot.event;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.*;
import insidebot.common.command.model.base.CommandReference;
import insidebot.common.command.service.CommandHandler;
import insidebot.data.entity.*;
import insidebot.data.service.*;
import insidebot.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.*;

@Component
public class MessageCreateHandler implements EventHandler<MessageCreateEvent>{
    @Autowired
    private MemberService memberService;

    @Autowired
    private UserService userService;

    @Autowired
    private CommandHandler commandHandler;

    @Override
    public Class<MessageCreateEvent> type(){
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<Void> onEvent(MessageCreateEvent event){
        User user = event.getMessage().getAuthor().orElse(null);
        if(DiscordUtil.isBot(user)) return Mono.empty();
        Message message = event.getMessage();
        Member member = event.getMember().orElse(null);
        Snowflake guildId = event.getGuildId().orElse(null);
        if(guildId == null || member == null) return Mono.empty();
        MessageInfo info = new MessageInfo();
        Snowflake userId = user.getId();
        LocalMember localMember = memberService.getOr(guildId, userId, LocalMember::new);

        if(localMember.user() == null){
            LocalUser localUser = userService.getById(userId);
            localUser.name(user.getUsername());
            localUser.discriminator(user.getDiscriminator());
            localMember.user(localUser);
        }

        localMember.effectiveName(member.getNickname().isPresent() ? member.getNickname().get() : member.getUsername());
        localMember.id(userId);
        localMember.lastSentMessage(Calendar.getInstance());
        localMember.addToSeq();

        info.member(localMember);
        info.id(message.getId());
        info.guildId(guildId);
        info.channelId(message.getChannelId());
        info.timestamp(Calendar.getInstance());

        info.content(MessageUtil.effectiveContent(message));

        CommandReference reference = new CommandReference()
                .localMember(localMember)
                .localUser(localMember.user())
                .member(member)
                .user(user);

        commandHandler.handleMessage(message.getContent(), reference, event);
        memberService.save(localMember);
        return Mono.empty();
    }
}
