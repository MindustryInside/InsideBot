package inside.command.common;

import inside.command.Command;
import inside.command.model.*;
import inside.util.Strings;
import reactor.core.publisher.Mono;

import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.*;

@DiscordCommand(key = {"random", "rand", "rnd"}, params = "command.random.params", description = "command.random.description")
public class RandomCommand extends Command{
    private static final Pattern rangePattern = Pattern.compile("^[(\\[]([-+]?[0-9]+)[,.;\\s]+([-+]?[0-9]+)[])]$");

    @Override
    public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
        String range = interaction.getOption(0)
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asString)
                .orElseThrow(IllegalStateException::new);

        Matcher matcher = rangePattern.matcher(range);
        if(!matcher.matches()){
            return messageService.err(env, "command.random.incorrect-format");
        }

        String fgroup = matcher.group(1);
        String sgroup = matcher.group(2);
        if(!Strings.canParseLong(fgroup) || !Strings.canParseLong(sgroup)){
            return messageService.err(env, "command.random.overflow");
        }

        boolean linc = range.startsWith("[");
        long lower = Strings.parseLong(fgroup);
        boolean hinc = range.endsWith("]");
        long higher = Strings.parseLong(sgroup);

        if(lower >= higher){
            return messageService.err(env, "command.random.equals");
        }

        String str = String.valueOf(ThreadLocalRandom.current().nextLong(
                lower + (!linc ? 1 : 0), higher + (hinc ? 1 : 0)));

        return messageService.text(env, str);
    }
}
