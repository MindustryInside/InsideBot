package inside.command.common;

import discord4j.core.spec.MessageEditSpec;
import inside.command.Command;
import inside.command.CommandEnvironment;
import inside.command.CommandInteraction;
import inside.command.DiscordCommand;
import inside.service.MessageService;
import org.reactivestreams.Publisher;

@DiscordCommand(key = "ping", description = "Отобразить время ответа бота.")
public class PingCommand extends Command {

    public PingCommand(MessageService messageService) {
        super(messageService);
    }

    @Override
    public Publisher<?> execute(CommandEnvironment env, CommandInteraction interaction) {
        long start = System.currentTimeMillis();
        return env.channel().createMessage("Подождите...")
                .flatMap(message -> message.edit(MessageEditSpec.builder()
                        .contentOrNull(String.format("Понг! %sмс", System.currentTimeMillis() - start))
                        .build()))
                .then();
    }
}
