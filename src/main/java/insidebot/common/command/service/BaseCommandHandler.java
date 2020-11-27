package insidebot.common.command.service;

import arc.struct.*;
import discord4j.core.event.domain.message.MessageCreateEvent;
import insidebot.common.command.model.base.*;
import insidebot.data.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;

import java.util.List;

public abstract class BaseCommandHandler{
    protected ObjectMap<String, Command> commands = new ObjectMap<>();
    protected String prefix;

    @Autowired
    protected List<CommandRunner> orderedCommands;

    public BaseCommandHandler(String prefix){
        this.prefix = prefix;
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

    public void setPrefix(String prefix){
        this.prefix = prefix;
    }

    public abstract CommandResponse handleMessage(String message, CommandReference reference, MessageCreateEvent event);

    public void removeCommand(String text){
        Command c = commands.get(text);
        if(c == null) return;
        commands.remove(text);
        orderedCommands.remove(c);
    }

    public Command register(CommandRunner runner){
        Command cmd = runner.compile();
        commands.put(cmd.text.toLowerCase(), cmd);
        orderedCommands.add(runner);
        return cmd;
    }

    public static class Command{
        public final String text;
        public final String paramText;
        public final String description;
        public final CommandParam[] params;
        final CommandRunner runner;

        @Autowired
        private MessageService messageService;

        public Command(String text, String paramText, String description, CommandRunner runner){
            this.text = text;
            this.paramText = paramText;
            this.runner = runner;
            this.description = messageService.get(description);

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
