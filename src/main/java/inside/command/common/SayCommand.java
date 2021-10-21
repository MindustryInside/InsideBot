package inside.command.common;

import inside.command.Command;
import inside.command.model.*;
import org.reactivestreams.Publisher;

// for an interesting quotes
@DiscordCommand(key = "say", params = "command.say.params", description = "command.say.description")
public class SayCommand extends Command{
    @Override
    public Publisher<?> execute(CommandEnvironment env, CommandInteraction interaction){
        String text = interaction.getOption("text")
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asString)
                .orElseThrow();

        return messageService.text(env, text);
    }
}
