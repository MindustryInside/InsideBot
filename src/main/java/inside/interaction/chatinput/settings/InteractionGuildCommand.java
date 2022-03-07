package inside.interaction.chatinput.settings;

import inside.interaction.chatinput.InteractionCommand;
import inside.service.MessageService;

// Просто для маркирования
public abstract class InteractionGuildCommand extends InteractionCommand {

    public InteractionGuildCommand(MessageService messageService) {
        super(messageService);
    }
}
