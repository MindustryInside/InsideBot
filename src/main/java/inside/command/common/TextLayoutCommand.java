package inside.command.common;

import inside.command.Command;
import inside.command.model.*;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

@DiscordCommand(key = "r", params = "command.text-layout.params", description = "command.text-layout.description")
public class TextLayoutCommand extends Command{
    private static final String[] engPattern;
    private static final String[] rusPattern;

    static{
        String eng = "Q-W-E-R-T-Y-U-I-O-P-A-S-D-F-G-H-J-K-L-Z-X-C-V-B-N-M";
        String rus = "Й-Ц-У-К-Е-Н-Г-Ш-Щ-З-Ф-Ы-В-А-П-Р-О-Л-Д-Я-Ч-С-М-И-Т-Ь";
        engPattern = (eng + "-" + eng.toLowerCase() + "-\\^-:-\\$-@-&-~-`-\\{-\\[-\\}-\\]-\"-'-<->-;-\\?-\\/-\\.-,-#").split("-");
        rusPattern = (rus + "-" + rus.toLowerCase() + "-:-Ж-;-\"-\\?-Ё-ё-Х-х-Ъ-ъ-Э-э-Б-Ю-ж-,-\\.-ю-б-№").split("-");
    }

    public static String text2rus(String text){
        for(int i = 0; i < engPattern.length; i++){
            text = text.replaceAll(engPattern[i], rusPattern[i]);
        }
        return text;
    }

    public static String text2eng(String text){
        for(int i = 0; i < rusPattern.length; i++){
            text = text.replaceAll(rusPattern[i], engPattern[i]);
        }
        return text;
    }

    @Override
    public Publisher<?> execute(CommandEnvironment env, CommandInteraction interaction){
        boolean en = interaction.getOption(0)
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asString)
                .map("en"::equalsIgnoreCase)
                .orElse(false);

        String text = interaction.getOption(1)
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asString)
                .orElseThrow();

        String res = en ? text2rus(text) : text2eng(text);
        return messageService.text(env, res.isBlank() ? messageService.get(env.context(), "message.placeholder") : res)
                .withMessageReference(env.message().getId());
    }
}
