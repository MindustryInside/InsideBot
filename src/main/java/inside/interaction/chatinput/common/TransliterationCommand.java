package inside.interaction.chatinput.common;

import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Message;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.annotation.ChatInputCommand;
import inside.interaction.chatinput.InteractionCommand;
import inside.util.MessageUtil;
import inside.util.Strings;
import org.reactivestreams.Publisher;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static inside.interaction.chatinput.common.LeetSpeakCommand.mapOf;

@ChatInputCommand(name = "tr", description = "Перевести текст в транслитерацию.")
public class TransliterationCommand extends InteractionCommand {
    static final Map<String, String> transliteration;

    static{
        transliteration = mapOf(
                "a", "а", "b", "б", "v", "в", "g", "г",
                "d", "д", "e", "е", "yo", "ё", "zh", "ж",
                "z", "з", "i", "и", "j", "й", "k", "к",
                "l", "л", "m", "м", "n", "н", "o", "о",
                "p", "п", "r", "р", "s", "с", "t", "т",
                "u", "у", "f", "ф", "h", "х", "x", "кс",
                "ts", "ц", "ch", "ч", "sh", "ш", "sh'", "щ",
                "\\`", "ъ", "y'", "ы", "'", "ь", "e\\`", "э",
                "yu", "ю", "ya", "я", "iy", "ий"
        );
    }

    static String translit(String text){
        UnaryOperator<String> get = s -> {
            String result = Optional.ofNullable(transliteration.get(s.toLowerCase()))
                    .or(transliteration.entrySet().stream()
                            .filter(entry -> entry.getValue().equalsIgnoreCase(s))
                            .map(Map.Entry::getKey)::findFirst)
                    .orElse("");

            return s.chars().anyMatch(Character::isUpperCase) ? result.toUpperCase() : result;
        };

        int len = text.length();
        if(len == 1){
            return get.apply(text);
        }

        StringBuilder result = new StringBuilder();
        for(int i = 0; i < len; ){
            String c = text.substring(i, i + (i <= len - 2 ? 2 : 1));
            String translited = get.apply(c);
            if(Strings.isEmpty(translited)){
                translited = get.apply(c.charAt(0) + "");
                result.append(Strings.isEmpty(translited) ? c.charAt(0) : translited);
                i++;
            }else{
                result.append(translited);
                i += 2;
            }
        }
        return result.toString();
    }

    public TransliterationCommand() {

        addOption(builder -> builder.name("text")
                .description("Текст на транслитерацию.")
                .required(true)
                .type(ApplicationCommandOption.Type.STRING.getValue()));
    }

    @Override
    public Publisher<?> execute(ChatInputInteractionEnvironment env) {
        return env.getOption("text")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map(TransliterationCommand::translit)
                .filter(Predicate.not(String::isBlank))
                .map(s -> MessageUtil.substringTo(s, Message.MAX_CONTENT_LENGTH))
                .map(env.event()::reply)
                .orElseGet(() -> err(env, "Не удалось перевести текст."));
    }
}
