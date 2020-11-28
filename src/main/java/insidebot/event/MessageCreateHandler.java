package insidebot.event;

import arc.util.Strings;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.MessageChannel;
import insidebot.Settings;
import insidebot.common.command.model.base.CommandReference;
import insidebot.common.command.service.BaseCommandHandler.*;
import insidebot.common.command.service.CommandHandler;
import insidebot.data.entity.*;
import insidebot.data.service.*;
import insidebot.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Calendar;

@Component
public class MessageCreateHandler implements EventHandler<MessageCreateEvent>{
    @Autowired
    private MemberService memberService;

    @Autowired
    private UserService userService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private CommandHandler commandHandler;

    @Autowired
    private Settings settings;

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
            LocalUser localUser = userService.getOr(userId, LocalUser::new);
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

        if(memberService.isAdmin(member)){
            handleResponse(commandHandler.handleMessage(message.getContent(), reference, event), message.getChannel().block());
        }
        memberService.save(localMember);
        return Mono.empty();
    }

    protected void handleResponse(CommandResponse response, MessageChannel channel){
        if(response.type == ResponseType.unknownCommand){
            int min = 0;
            Command closest = null;

            for(Command command : commandHandler.commandList()){
                int dst = Strings.levenshtein(command.text, response.runCommand);
                if(dst < 3 && (closest == null || dst < min)){
                    min = dst;
                    closest = command;
                }
            }

            if(closest != null){
                messageService.err(channel, messageService.format("command.response.found-closest", closest.text));
            }else{
                messageService.err(channel, messageService.format("command.response.unknown", settings.prefix));
            }
        }else if(response.type == ResponseType.manyArguments){
            messageService.err(channel, messageService.get("command.response.many-arguments"),
                               messageService.format("command.response.many-arguments.text",
                                                     settings.prefix, response.command.text, response.command.paramText));
        }else if(response.type == ResponseType.fewArguments){
            messageService.err(channel, messageService.get("command.response.few-arguments"),
                               messageService.format("command.response.few-arguments.text",
                                                     settings.prefix, response.command.text));
        }
    }
}
