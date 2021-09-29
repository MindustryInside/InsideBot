package inside.interaction.chatinput;

import java.util.*;

public interface InteractionOwnerCommand extends InteractionChatInputCommand{

    void addSubCommand(InteractionChatInputCommand subcommand);

    Optional<InteractionChatInputCommand> getSubCommand(String name);

    Map<String, InteractionChatInputCommand> getSubCommands();
}
