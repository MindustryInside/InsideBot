package inside.interaction.chatinput;

import inside.service.MessageService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InteractionCommandHolder {

    private final Map<String, InteractionCommand> commands;

    private InteractionCommandHolder(Map<String, InteractionCommand> commands) {
        this.commands = commands;
    }

    public static Builder builder(MessageService messageService) {
        return new Builder(messageService);
    }

    public Map<String, InteractionCommand> getCommands() {
        return commands;
    }

    public Optional<InteractionCommand> getCommand(String value) {
        return Optional.ofNullable(commands.get(value));
    }

    public static class Builder {
        private final MessageService messageService;
        private final Map<String, InteractionCommand> commands = new HashMap<>();

        private Builder(MessageService messageService) {
            this.messageService = messageService;
        }

        public Builder addCommand(InteractionCommand interactionCommand) {
            commands.put(messageService.get(interactionCommand.info.nameCode() + ".name"), interactionCommand);
            return this;
        }

        public InteractionCommandHolder build() {
            return new InteractionCommandHolder(commands);
        }
    }
}
