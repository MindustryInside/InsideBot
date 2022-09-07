package inside.interaction.chatinput;

import discord4j.discordjson.json.ImmutableApplicationCommandRequest;

public abstract class InteractionSubcommand<T extends InteractionCommand> extends InteractionCommand {

    protected final T owner;

    protected InteractionSubcommand(T owner) {
        super(owner.messageService);
        this.owner = owner;
    }

    @Override
    public final ImmutableApplicationCommandRequest asRequest() {
        throw new UnsupportedOperationException();
    }
}
