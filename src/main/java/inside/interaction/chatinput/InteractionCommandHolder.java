package inside.interaction.chatinput;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class InteractionCommandHolder {

    private final Map<String, InteractionCommand> commands;

    private InteractionCommandHolder(Map<String, InteractionCommand> commands) {
        this.commands = Collections.unmodifiableMap(commands);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, InteractionCommand> getCommands() {
        return commands;
    }

    public Optional<InteractionCommand> getCommand(String value) {
        return Optional.ofNullable(commands.get(value));
    }

    public static class Builder {
        private final Map<String, InteractionCommand> commands = new LinkedHashMap<>();

        public Map<String, InteractionCommand> getCommands() {
            return Collections.unmodifiableMap(commands);
        }

        public Builder addCommand(InteractionCommand interactionCommand) {
            commands.put(interactionCommand.getName(), interactionCommand);
            return this;
        }

        public InteractionCommandHolder build() {
            return new InteractionCommandHolder(commands);
        }
    }
}
