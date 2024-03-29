package inside.command;

import inside.command.model.*;
import inside.service.MessageService;
import inside.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.util.context.Context;

import java.util.*;

import static inside.util.ContextUtil.KEY_LOCALE;

@Component
public final class CommandHolder{

    private final MessageService messageService;

    private final Map<String[], Command> commands = new LinkedHashMap<>();

    private final Map<Command, CommandInfo> commandInfo = new LinkedHashMap<>();

    public CommandHolder(@Autowired MessageService messageService){
        this.messageService = messageService;
    }

    private static <T> boolean contains(T[] arr, T value){
        for(T t : arr){
            if(Objects.equals(t, value)){
                return true;
            }
        }
        return false;
    }

    @Autowired(required = false)
    private void registerCommands(List<Command> commands){
        for(Command command : commands){
            CommandInfo info = compile(command);
            this.commands.put(info.key(), command);
            commandInfo.put(command, info);
        }
    }

    private CommandInfo compile(Command command){
        DiscordCommand meta = command.getAnnotation();
        Preconditions.requireState(meta.key().length > 0);
        commandInfo.forEach((command0, commandInfo0) -> {
            for(String s : meta.key()){
                Preconditions.requireState(s.matches("^[\\S-]{1,32}$"), "Incorrect command alias '" + s + "' format!");

                for(String s1 : commandInfo0.key()){
                    Preconditions.requireState(!s1.equals(s), () -> "Duplicate command alias '" + s + "' in '" +
                            command.getClass().getCanonicalName() + "' and '" + command0.getClass().getCanonicalName() + "'!");
                }
            }
        });

        // get 'en' parameter key for validation and option search
        String paramText = messageService.get(Context.of(KEY_LOCALE, messageService.getDefaultLocale()), meta.params());
        String[] psplit = paramText.split("(?<=(\\]|>))\\s+(?=(\\[|<))");
        CommandParam[] params = CommandParam.empty;
        if(!paramText.isBlank()){
            params = new CommandParam[psplit.length];
            boolean hadOptional = false;

            for(int i = 0; i < params.length; i++){
                String param = psplit[i].trim();
                Preconditions.requireState(!Strings.isEmpty(param), "Malformed param '" + param + "'");

                char l = param.charAt(0), r = param.charAt(param.length() - 1);
                boolean optional, variadic = false;

                if(l == '<' && r == '>'){
                    Preconditions.requireState(!hadOptional, "Can't have non-optional param after optional param!");
                    optional = false;
                }else if(l == '[' && r == ']'){
                    optional = hadOptional = true;
                }else{
                    throw new IllegalArgumentException("Malformed param '" + param + "'");
                }

                String fname = param.substring(1, param.length() - 1);
                if(fname.endsWith("...")){
                    Preconditions.requireState(i != param.length() - 1, "A variadic parameter should be the last parameter!");
                    fname = fname.substring(0, fname.length() - 3);
                    variadic = true;
                }

                params[i] = new CommandParam(fname, optional, variadic);
            }
        }

        return new CommandInfo(meta.key(), meta.params(), meta.description(),
                params, meta.permissions(), meta.category());
    }

    public Map<String[], Command> getCommandsMap(){
        return Collections.unmodifiableMap(commands);
    }

    public Map<Command, CommandInfo> getCommandInfoMap(){
        return Collections.unmodifiableMap(commandInfo);
    }

    public Optional<Command> getCommand(String key){
        Objects.requireNonNull(key, "key");
        return commands.entrySet().stream()
                .filter(entry -> contains(entry.getKey(), key))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    public Optional<CommandInfo> getCommandInfo(String key){
        Objects.requireNonNull(key, "key");
        return commands.entrySet().stream()
                .filter(entry -> contains(entry.getKey(), key))
                .map(Map.Entry::getValue)
                .map(commandInfo::get)
                .findFirst();
    }
}
