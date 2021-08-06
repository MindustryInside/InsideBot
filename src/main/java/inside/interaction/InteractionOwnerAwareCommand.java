package inside.interaction;

public interface InteractionOwnerAwareCommand<T extends InteractionOwnerCommand> extends InteractionCommand{

    T getOwner();
}
