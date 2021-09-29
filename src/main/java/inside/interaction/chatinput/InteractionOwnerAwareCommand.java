package inside.interaction.chatinput;

public interface InteractionOwnerAwareCommand<T extends InteractionOwnerCommand> extends InteractionChatInputCommand{

    T getOwner();
}
