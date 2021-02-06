package inside.common.command.service;

import arc.struct.ObjectMap;
import inside.Settings;
import inside.common.command.model.base.*;
import inside.data.service.DiscordService;
import inside.data.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

public abstract class BaseCommandHandler{
    @Autowired
    protected EntityRetriever entityRetriever;

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
        commands.forEach(cmd -> {
            CommandInfo command = cmd.compile();
            this.commands.put(command.text, cmd);
        });
    }

    public ObjectMap<String, Command> commands(){
        return commands;
    }

    public List<CommandInfo> commandList(){
        return handlers.stream().map(Command::compile).collect(Collectors.toUnmodifiableList());
    }

    public abstract Mono<Void> handleMessage(final String message, final CommandReference reference);
}
