package inside.interaction.chatinput.settings;

import inside.data.EntityRetriever;
import inside.service.MessageService;

import java.util.Objects;

abstract class InteractionConfigCommand extends InteractionGuildCommand {

    protected final EntityRetriever entityRetriever;

    public InteractionConfigCommand(MessageService messageService, EntityRetriever entityRetriever) {
        super(messageService);
        this.entityRetriever = Objects.requireNonNull(entityRetriever, "entityRetriever");
    }
}
