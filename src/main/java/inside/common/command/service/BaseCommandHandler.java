package inside.common.command.service;

import arc.struct.ObjectMap;
import discord4j.core.event.domain.message.MessageCreateEvent;
import inside.Settings;
import inside.common.command.model.base.CommandInfo;
import inside.common.command.model.base.*;
import inside.common.services.DiscordService;
import inside.data.service.*;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

public abstract class BaseCommandHandler{
    @Autowired
    protected GuildService guildService;

    @Autowired
    protected DiscordService discordService;

    @Autowired
    protected MessageService messageService;

    @Autowired
    protected Settings settings;

    protected ObjectMap<String, Command> commands = new ObjectMap<>();

    protected List<Command> handlers;

    @Autowired(required = false)
    public void init(List<Command> commands){
        handlers = commands;
        commands.forEach(c -> {
            CommandInfo command = c.compile();
            this.commands.put(command.text, c);
        });
    }

    public ObjectMap<String, Command> commands(){
        return commands;
    }

    public List<CommandInfo> commandList(){
        return handlers.stream().map(Command::compile).collect(Collectors.toUnmodifiableList());
    }

    public abstract Publisher<Void> handleMessage(String message, CommandReference reference);
}
