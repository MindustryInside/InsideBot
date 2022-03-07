package inside.interaction.chatinput.settings;

import inside.data.EntityRetriever;

import java.util.Objects;

abstract class InteractionConfigCommand extends InteractionGuildCommand {

    protected final EntityRetriever entityRetriever;

    public InteractionConfigCommand(EntityRetriever entityRetriever) {
        this.entityRetriever = Objects.requireNonNull(entityRetriever, "entityRetriever");
    }
}
