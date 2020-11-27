package insidebot.common.command.model;

import discord4j.core.event.domain.message.MessageCreateEvent;
import insidebot.Settings;
import insidebot.common.command.model.base.*;
import insidebot.common.command.service.CommandHandler;
import insidebot.data.service.MessageService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public final class Commands{
    @Autowired
    private MessageService messageService;

    @Autowired
    private CommandHandler handler;

    @Autowired
    private Settings settings;

    @Autowired
    private Logger log;

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

    @DiscordCommand(key = "mute", description = "command.mute.description")
    public class MuteCommand implements CommandRunner{
        @Override
        public Mono<Void> execute(CommandReference reference, MessageCreateEvent event, String[] args){
            return null;
        }
    }

    @DiscordCommand(key = "delete", description = "command.delete.description")
    public class DeleteCommand implements CommandRunner{
        @Override
        public Mono<Void> execute(CommandReference reference, MessageCreateEvent event, String[] args){
            return null;
        }
    }

    @DiscordCommand(key = "warn", description = "command.warn.description")
    public class WarnCommand implements CommandRunner{
        @Override
        public Mono<Void> execute(CommandReference reference, MessageCreateEvent event, String[] args){
            return null;
        }
    }

    @DiscordCommand(key = "warnings", description = "command.warnings.description")
    public class WarningsCommand implements CommandRunner{
        @Override
        public Mono<Void> execute(CommandReference reference, MessageCreateEvent event, String[] args){
            return null;
        }
    }

    @DiscordCommand(key = "unwarn", description = "command.unwarn.description")
    public class UnwarnCommand implements CommandRunner{
        @Override
        public Mono<Void> execute(CommandReference reference, MessageCreateEvent event, String[] args){
            return null;
        }
    }

    @DiscordCommand(key = "unmute", description = "command.unmute.description")
    public class UnmuteCommand implements CommandRunner{
        @Override
        public Mono<Void> execute(CommandReference reference, MessageCreateEvent event, String[] args){
            return null;
        }
    }
}
