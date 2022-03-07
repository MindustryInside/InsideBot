package inside.interaction.chatinput.common;

import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Message;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.annotation.ChatInputCommand;
import inside.interaction.chatinput.InteractionCommand;
import inside.util.MessageUtil;
import org.reactivestreams.Publisher;

@ChatInputCommand(name = "r", description = "Изменить раскладку текста.")
public class TextLayoutCommand extends InteractionCommand {
    static final String[] engPattern;
    static final String[] rusPattern;

    static {
        String eng = "Q-W-E-R-T-Y-U-I-O-P-A-S-D-F-G-H-J-K-L-Z-X-C-V-B-N-M";
        String rus = "Й-Ц-У-К-Е-Н-Г-Ш-Щ-З-Ф-Ы-В-А-П-Р-О-Л-Д-Я-Ч-С-М-И-Т-Ь";
        engPattern = (eng + "-" + eng.toLowerCase() + "-\\^-:-\\$-@-&-~-`-\\{-\\[-\\}-\\]-\"-'-<->-;-\\?-\\/-\\.-,-#").split("-");
        rusPattern = (rus + "-" + rus.toLowerCase() + "-:-Ж-;-\"-\\?-Ё-ё-Х-х-Ъ-ъ-Э-э-Б-Ю-ж-,-\\.-ю-б-№").split("-");
    }

    static String text2rus(String text) {
        for (int i = 0; i < engPattern.length; i++) {
            text = text.replaceAll(engPattern[i], rusPattern[i]);
        }
        return text;
    }

    static String text2eng(String text) {
        for (int i = 0; i < rusPattern.length; i++) {
            text = text.replaceAll(rusPattern[i], engPattern[i]);
        }
        return text;
    }

    public TextLayoutCommand() {

        addOption(builder -> builder.name("type")
                .description("Тип раскладки c которой нужно переводить текст.")
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .required(true)
                .addChoice(ApplicationCommandOptionChoiceData.builder()
                        .name("Английская")
                        .value("en")
                        .build())
                .addChoice(ApplicationCommandOptionChoiceData.builder()
                        .name("Русская")
                        .value("ru")
                        .build()));

        addOption(builder -> builder.name("text")
                .description("Текст для перевода.")
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .required(true));
    }

    @Override
    public Publisher<?> execute(ChatInputInteractionEnvironment env) {
        boolean russian = env.getOption("type")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map("ru"::equals)
                .orElseThrow();

        return env.getOption("text")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map(str -> russian ? text2eng(str) : text2rus(str))
                .map(s -> MessageUtil.substringTo(s, Message.MAX_CONTENT_LENGTH))
                .map(env.event()::reply)
                .orElseGet(() -> err(env, "Не удалось перевести текст."));
    }
}
