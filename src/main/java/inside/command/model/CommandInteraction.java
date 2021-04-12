package inside.command.model;

import java.util.*;

public class CommandInteraction{
    private final String commandName;
    private final List<CommandOption> options;

    public CommandInteraction(String commandName, List<CommandOption> options){
        this.commandName = commandName;
        this.options = options;
    }

    public String getCommandName(){
        return commandName;
    }

    public List<CommandOption> getOptions(){
        return options;
    }

    public Optional<CommandOption> getOption(int index){
        return Optional.ofNullable(options.get(index));
    }

    public Optional<CommandOption> getOption(String name){
        return options.stream()
                .filter(option -> option.getName().equals(name))
                .findFirst();
    }

    @Override
    public String toString(){
        return "CommandInteraction{" +
                "commandName='" + commandName + '\'' +
                ", options=" + options +
                '}';
    }
}
