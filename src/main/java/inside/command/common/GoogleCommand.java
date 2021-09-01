package inside.command.common;

import inside.command.Command;
import inside.command.model.*;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@DiscordCommand(key = "google", params = "command.google.params", description = "command.google.description")
public class GoogleCommand extends Command{
    @Override
    public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
        String text = interaction.getOption(0)
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asString)
                .orElseThrow(IllegalStateException::new);

        return messageService.text(env, "https://www.google.com/search?q=" + URLEncoder.encode(text, StandardCharsets.UTF_8));
    }
}
