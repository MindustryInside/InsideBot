package inside.interaction.chatinput;

public abstract class InteractionSubcommand<T extends InteractionCommand> extends InteractionCommand {

    protected final T owner;

    protected InteractionSubcommand(T owner) {
        this.owner = owner;
    }

    public T getOwner() {
        return owner;
    }
}
