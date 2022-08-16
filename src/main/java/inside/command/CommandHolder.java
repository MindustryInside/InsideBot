package inside.command;

import inside.util.Preconditions;
import inside.util.Strings;

import java.util.*;

public final class CommandHolder {

    private final Map<String[], Command> commands;
    private final Map<Command, CommandInfo> commandInfo;

    public CommandHolder(Map<String[], Command> commands, Map<Command, CommandInfo> commandInfo) {
        this.commands = Objects.requireNonNull(commands);
        this.commandInfo = Objects.requireNonNull(commandInfo);
    }

    public static CommandHolder.Builder builder() {
        return new CommandHolder.Builder();
    }

    public Map<String[], Command> getCommandsMap() {
        return Collections.unmodifiableMap(commands);
    }

    public Map<Command, CommandInfo> getCommandInfoMap() {
        return Collections.unmodifiableMap(commandInfo);
    }

    public Optional<Command> getCommand(String key) {
        Objects.requireNonNull(key, "key");
        return commands.entrySet().stream()
                .filter(entry -> Arrays.binarySearch(entry.getKey(), key) >= 0)
                .map(Map.Entry::getValue)
                .findFirst();
    }

    public Optional<CommandInfo> getCommandInfo(String key) {
        Objects.requireNonNull(key, "key");
        return commands.entrySet().stream()
                .filter(entry -> Arrays.binarySearch(entry.getKey(), key) >= 0)
                .map(Map.Entry::getValue)
                .map(commandInfo::get)
                .findFirst();
    }

    public static class Builder {
        private final Map<String[], Command> commands = new HashMap<>();
        private final Map<Command, CommandInfo> commandInfo = new HashMap<>();

        private CommandInfo compile(Command command) {
            DiscordCommand meta = command.getAnnotation();
            Preconditions.requireState(meta.key().length > 0);
            commandInfo.forEach((command0, commandInfo0) -> {
                for (String s : meta.key()) {
                    Preconditions.requireState(s.matches("^[\\S-]{1,32}$"), "Incorrect command alias '" + s + "' format!");

                    for (String s1 : commandInfo0.key()) {
                        Preconditions.requireState(!s1.equals(s), () -> "Duplicate command alias '" + s + "' in '" +
                                command.getClass().getCanonicalName() + "' and '" + command0.getClass().getCanonicalName() + "'!");
                    }
                }
            });

            String paramText = meta.params();
            String[] psplit = paramText.split("(?<=(\\]|>))\\s+(?=(\\[|<))");
            CommandParam[] params = CommandParam.empty;
            if (!paramText.isBlank()) {
                params = new CommandParam[psplit.length];
                boolean hadOptional = false;

                for (int i = 0; i < params.length; i++) {
                    String param = psplit[i].trim();
                    Preconditions.requireState(!Strings.isEmpty(param), "Malformed param '" + param + "'");

                    char l = param.charAt(0), r = param.charAt(param.length() - 1);
                    boolean optional, variadic = false;

                    if (l == '<' && r == '>') {
                        Preconditions.requireState(!hadOptional, "Can't have non-optional param after optional param!");
                        optional = false;
                    } else if (l == '[' && r == ']') {
                        optional = hadOptional = true;
                    } else {
                        throw new IllegalArgumentException("Malformed param '" + param + "'");
                    }

                    String fname = param.substring(1, param.length() - 1);
                    if (fname.endsWith("...")) {
                        Preconditions.requireState(i != param.length() - 1, "A variadic parameter should be the last parameter!");
                        fname = fname.substring(0, fname.length() - 3);
                        variadic = true;
                    }

                    params[i] = new CommandParam(fname, optional, variadic);
                }
            }

            Arrays.setAll(meta.key(), i -> meta.key()[i] = meta.key()[i].toLowerCase(Locale.ROOT));
            Arrays.sort(meta.key());
            return new CommandInfo(meta.key(), meta.params(), meta.description(),
                    params, meta.permissions(), meta.category());
        }

        public Map<String[], Command> getCommands() {
            return Collections.unmodifiableMap(commands);
        }

        public Builder addCommand(Command command) {
            CommandInfo info = compile(command);
            this.commands.put(info.key(), command);
            commandInfo.put(command, info);
            return this;
        }

        public CommandHolder build() {
            return new CommandHolder(commands, commandInfo);
        }
    }
}
