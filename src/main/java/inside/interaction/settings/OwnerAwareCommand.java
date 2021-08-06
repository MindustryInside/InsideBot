package inside.interaction.settings;

import inside.interaction.*;

import java.util.Objects;

public abstract class OwnerAwareCommand<T extends InteractionOwnerCommand>
        extends SettingsCommand
        implements InteractionOwnerAwareCommand<T>{

    protected final T owner;

    protected OwnerAwareCommand(T owner){
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    @Override
    public T getOwner(){
        return owner;
    }
}
