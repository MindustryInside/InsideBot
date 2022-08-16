package inside.interaction.chatinput;

import discord4j.discordjson.json.ApplicationCommandRequest;

public abstract class InteractionSubcommand<T extends InteractionCommand> extends InteractionCommand {

    protected final T owner;

    protected InteractionSubcommand(T owner) {
        super(owner.messageService);
        this.owner = owner;
    }

    @Override
    public ApplicationCommandRequest getRequest() {
        return super.getRequest();
    }
}
