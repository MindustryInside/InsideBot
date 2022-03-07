package inside.interaction.chatinput;

import inside.service.MessageService;

// Просто для маркирования
public abstract class InteractionGuildCommand extends InteractionCommand {

    public InteractionGuildCommand(MessageService messageService) {
        super(messageService);
    }
}
