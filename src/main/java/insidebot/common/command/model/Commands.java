package insidebot.common.command.model;

import arc.Events;
import arc.math.Mathf;
import arc.util.Strings;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.*;
import insidebot.*;
import insidebot.event.dispatcher.EventType;
import insidebot.event.dispatcher.EventType.*;
import insidebot.common.command.model.base.*;
import insidebot.common.command.service.CommandHandler;
import insidebot.common.services.DiscordService;
import insidebot.data.entity.LocalMember;
import insidebot.data.service.*;
import insidebot.util.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public final class Commands{
    @Autowired
    private DiscordService discordService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private CommandHandler handler;

    @Autowired
    private Settings settings;

    @Autowired
    private Logger log;

    private final String[] warningStrings = {messageService.get("command.first"), messageService.get("command.second"), messageService.get("command.third")};

    @DiscordCommand(key = "help", description = "command.help.description")
    public class HelpCommand implements CommandRunner{
        @Override
        public Mono<Void> execute(CommandReference reference, MessageCreateEvent event, String[] args){
            StringBuilder builder = new StringBuilder();

            handler.commandFlux().map(CommandRunner::compile).subscribe(command -> {
                builder.append(settings.prefix);
                builder.append("**");
                builder.append(command.text);
                builder.append("**");
                if(command.params.length > 0){
                    builder.append(" *");
                    builder.append(command.paramText);
                    builder.append('*');
                }
                builder.append(" - ");
                builder.append(command.description);
                builder.append('\n');
            });

            return messageService.info(event.getMessage().getChannel().block(), messageService.get("command.help"), builder.toString());
        }
    }

    @DiscordCommand(key = "mute", params = "<@user> <delayDays> [reason...]", description = "command.mute.description")
    public class MuteCommand implements CommandRunner{
        @Override
        public Mono<Void> execute(CommandReference reference, MessageCreateEvent event, String[] args){
            MessageChannel channel = event.getMessage().getChannel().block();
            if(!MessageUtil.canParseInt(args[1])){
                messageService.err(channel, messageService.get("command.incorrect-number"));
                return Mono.empty();
            }

            try{
                int delayDays = Strings.parseInt(args[1]);
                LocalMember info = memberService.get(event.getGuildId().orElseThrow(RuntimeException::new), MessageUtil.parseUserId(args[0]));
                Member m = discordService.gateway().getMemberById(info.guildId(), info.id()).block();

                if(DiscordUtil.isBot(m)){
                    messageService.err(channel, messageService.get("command.user-is-bot"));
                    return Mono.empty();
                }

                if(memberService.isAdmin(m)){
                    messageService.err(channel, messageService.get("command.user-is-admin"));
                    return Mono.empty();
                }

                if(reference.member().equals(m)){
                    messageService.err(channel, messageService.get("command.mute.self-user"));
                    return Mono.empty();
                }

                discordService.eventListener().publish(new MemberMuteEvent(event.getGuild().block(), info, delayDays));
            }catch(Exception e){
                messageService.err(channel, messageService.get("command.incorrect-name"));
            }

            return Mono.empty();
        }
    }

    @DiscordCommand(key = "delete", params = "<amount>", description = "command.delete.description")
    public class DeleteCommand implements CommandRunner{
        @Override
        public Mono<Void> execute(CommandReference reference, MessageCreateEvent event, String[] args){
            TextChannel channel = event.getMessage().getChannel().cast(TextChannel.class).block();
            if(!MessageUtil.canParseInt(args[0])){
                messageService.err(channel, messageService.get("command.incorrect-number"));
                return Mono.empty();
            }

            int number = Integer.parseInt(args[0]);
            if(number >= 100){
                messageService.err(channel, messageService.format("command.limit-number", 100));
                return Mono.empty();
            }

            List<Message> history = channel.getMessagesBefore(event.getMessage().getId())
                                           .limitRequest(number)
                                           .collectList()
                                           .block();

            if(history == null || (history.isEmpty() && number > 0)){
                messageService.err(channel, messageService.get("command.hist-error"));
                return Mono.empty();
            }

            discordService.eventListener().publish(new MessageClearEvent(event.getGuild().block(), history, reference.user(), channel, number));
            return Mono.empty();
        }
    }

    @DiscordCommand(key = "warn", params = "<@user> [reason...]", description = "command.warn.description")
    public class WarnCommand implements CommandRunner{
        @Override
        public Mono<Void> execute(CommandReference reference, MessageCreateEvent event, String[] args){
            MessageChannel channel = event.getMessage().getChannel().block();
            try{
                LocalMember info = memberService.get(event.getGuildId().orElseThrow(RuntimeException::new), MessageUtil.parseUserId(args[0]));
                Member m = discordService.gateway().getMemberById(info.guildId(), info.id()).block();

                if(DiscordUtil.isBot(m)){
                    messageService.err(channel, messageService.get("command.user-is-bot"));
                    return Mono.empty();
                }

                if(memberService.isAdmin(reference.member())){
                    messageService.err(channel, messageService.get("command.user-is-admin"));
                    return Mono.empty();
                }

                if(reference.member().equals(m)){
                    messageService.err(channel, messageService.get("command.warn.self-user"));
                    return Mono.empty();
                }

                int warnings = info.addWarn();

                messageService.text(channel, messageService.format("message.warn", m.getUsername(), warningStrings[Mathf.clamp(warnings - 1, 0, warningStrings.length - 1)]));

                if(warnings >= 3){
                    event.getGuild().flatMap(g -> g.ban(m.getId(), b -> b.setDeleteMessageDays(0))).block();
                }else{
                    memberService.save(info);
                }
            }catch(Exception e){
                messageService.err(channel, messageService.get("command.incorrect-name"));
            }

            return Mono.empty();
        }
    }

    @DiscordCommand(key = "warnings", params = "<@user>", description = "command.warnings.description")
    public class WarningsCommand implements CommandRunner{
        @Override
        public Mono<Void> execute(CommandReference reference, MessageCreateEvent event, String[] args){
            MessageChannel channel = event.getMessage().getChannel().block();
            try{
                LocalMember info = memberService.get(event.getGuildId().orElseThrow(RuntimeException::new), MessageUtil.parseUserId(args[0]));
                int warnings = info.warns();
                messageService.text(channel, messageService.format("command.warnings", info.effectiveName(), warnings, warnings == 1 ? messageService.get("command.warn") : messageService.get("command.warns")));
            }catch(Exception e){
                messageService.err(channel, messageService.get("command.incorrect-name"));
            }

            return Mono.empty();
        }
    }

    @DiscordCommand(key = "unwarn", params = "<@user> [count]", description = "command.unwarn.description")
    public class UnwarnCommand implements CommandRunner{
        @Override
        public Mono<Void> execute(CommandReference reference, MessageCreateEvent event, String[] args){
            MessageChannel channel = event.getMessage().getChannel().block();
            if(args.length > 1 && !MessageUtil.canParseInt(args[1])){
                messageService.text(channel, messageService.get("command.incorrect-number"));
                return Mono.empty();
            }

            int warnings = args.length > 1 ? Strings.parseInt(args[1]) : 1;

            try{
                LocalMember info = memberService.get(event.getGuildId().orElseThrow(RuntimeException::new), MessageUtil.parseUserId(args[0]));
                info.warns(info.warns() - warnings);
                messageService.text(channel, messageService.format("command.unwarn", info.effectiveName(), warnings, warnings == 1 ? messageService.get("command.warn") : messageService.get("command.warns")));
                memberService.save(info);
            }catch(Exception e){
                messageService.err(channel, messageService.get("command.incorrect-name"));
            }

            return Mono.empty();
        }
    }

    @DiscordCommand(key = "unmute", params = "<@user>", description = "command.unmute.description")
    public class UnmuteCommand implements CommandRunner{
        @Override
        public Mono<Void> execute(CommandReference reference, MessageCreateEvent event, String[] args){
            try{
                LocalMember info = memberService.get(event.getGuildId().orElseThrow(RuntimeException::new), MessageUtil.parseUserId(args[0]));
                discordService.eventListener().publish(new MemberUnmuteEvent(event.getGuild().block(), info));
            }catch(Exception e){
                messageService.err(event.getMessage().getChannel().block(), messageService.get("command.incorrect-name"));
            }

            return Mono.empty();
        }
    }
}
