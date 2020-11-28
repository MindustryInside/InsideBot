package insidebot.common.command.service;

import arc.struct.ObjectMap;
import discord4j.core.event.domain.message.MessageCreateEvent;
import insidebot.common.command.model.base.*;
import insidebot.data.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;

import javax.annotation.PostConstruct;
import java.util.*;

public abstract class BaseCommandHandler{
    protected ObjectMap<String, Command> commands = new ObjectMap<>();

    @Autowired
    protected GuildService guildService;

    @Autowired(required = false)
    protected List<CommandRunner> orderedCommands = new ArrayList<>();

    @PostConstruct
    public void init(){
        orderedCommands.forEach(c -> {
            Command command = c.compile();
            commands.put(command.text, command);
        });
    }

    public ObjectMap<String, Command> commands(){
        return commands;
    }

    public List<Command> commandList(){
        return commandFlux().map(CommandRunner::compile).collectList().block();
    }

    public Flux<CommandRunner> commandFlux(){
        return Flux.fromIterable(orderedCommands);
    }

    public abstract CommandResponse handleMessage(String message, CommandReference reference, MessageCreateEvent event);

    public static class Command{
        public final String text;
        public final String paramText;
        public final CommandParam[] params;
        protected final CommandRunner runner;
        public String description;

        public Command(String text, String paramText, String description, CommandRunner runner){
            this.text = text;
            this.paramText = paramText;
            this.runner = runner;
            this.description = description;

            String[] psplit = paramText.split(" ");
            if(paramText.length() == 0){
                params = new CommandParam[0];
            }else{
                params = new CommandParam[psplit.length];

                boolean hadOptional = false;

                for(int i = 0; i < params.length; i++){
                    String param = psplit[i];

                    if(param.length() <= 2) throw new IllegalArgumentException("Malformed param '" + param + "'");

                    char l = param.charAt(0), r = param.charAt(param.length() - 1);
                    boolean optional, variadic = false;

                    if(l == '<' && r == '>'){
                        if(hadOptional){
                            throw new IllegalArgumentException("Can't have non-optional param after optional param!");
                        }
                        optional = false;
                    }else if(l == '[' && r == ']'){
                        optional = true;
                    }else{
                        throw new IllegalArgumentException("Malformed param '" + param + "'");
                    }

                    if(optional) hadOptional = true;

                    String fname = param.substring(1, param.length() - 1);
                    if(fname.endsWith("...")){
                        if(i != params.length - 1){
                            throw new IllegalArgumentException("A variadic parameter should be the last parameter!");
                        }

                        fname = fname.substring(0, fname.length() - 3);
                        variadic = true;
                    }

                    params[i] = new BaseCommandHandler.CommandParam(fname, optional, variadic);

                }
            }
        }
    }

    public static class CommandParam{
        public final String name;
        public final boolean optional;
        public final boolean variadic;

        public CommandParam(String name, boolean optional, boolean variadic){
            this.name = name;
            this.optional = optional;
            this.variadic = variadic;
        }

    }

    public static class CommandResponse{
        public final ResponseType type;
        public final Command command;
        public final String runCommand;

        public CommandResponse(BaseCommandHandler.ResponseType type, Command command, String runCommand){
            this.type = type;
            this.command = command;
            this.runCommand = runCommand;
        }
    }

    public enum ResponseType{
        noCommand, unknownCommand, fewArguments, manyArguments, valid;
    }
}
