package inside.interaction;

import java.util.*;

public interface InteractionOwnerCommand extends InteractionCommand{

    void addSubCommand(InteractionCommand subcommand);

    Optional<InteractionCommand> getSubCommand(String name);

    Map<String, InteractionCommand> getSubCommands();
}
