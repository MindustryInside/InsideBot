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
import inside.util.Preconditions;
import inside.util.Strings;
import org.reactivestreams.Publisher;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

@ChatInputCommand(name = "1337", description = "Перевести текст в leet speak.")
public class LeetSpeakCommand extends InteractionCommand {
    static final Map<String, String> rusLeetSpeak;
    static final Map<String, String> engLeetSpeak;

    static {
        rusLeetSpeak = mapOf(
                "а", "4", "б", "6", "в", "8", "г", "g",
                "д", "d", "е", "3", "ё", "3", "ж", "zh",
                "з", "e", "и", "i", "й", "\\`i", "к", "k",
                "л", "l", "м", "m", "н", "n", "о", "0",
                "п", "p", "р", "r", "с", "c", "т", "7",
                "у", "y", "ф", "f", "х", "x", "ц", "u,",
                "ч", "ch", "ш", "w", "щ", "w,", "ъ", "\\`ь",
                "ы", "ьi", "ь", "ь", "э", "э", "ю", "10",
                "я", "9"
        );

        engLeetSpeak = mapOf(
                "a", "4", "b", "8", "c", "c", "d", "d",
                "e", "3", "f", "ph", "g", "9", "h", "h",
                "i", "1", "j", "g", "k", "k", "l", "l",
                "m", "m", "n", "n", "o", "0", "p", "p",
                "q", "q", "r", "r", "s", "5", "t", "7",
                "u", "u", "v", "v", "w", "w", "x", "x",
                "y", "y", "z", "2"
        );
    }

    @SuppressWarnings("unchecked")
    static <K, V> Map<K, V> mapOf(Object... values){
        Objects.requireNonNull(values, "values");
        Preconditions.requireArgument((values.length & 1) == 0, "length is odd");
        Map<K, V> map = new HashMap<>();

        for(int i = 0; i < values.length / 2; ++i){
            map.put((K)values[i * 2], (V)values[i * 2 + 1]);
        }

        return Map.copyOf(map);
    }

    static String leeted(String text, boolean russian) {
        Map<String, String> map = russian ? rusLeetSpeak : engLeetSpeak;
        UnaryOperator<String> get = s -> {
            String result = Optional.ofNullable(map.get(s.toLowerCase()))
                    .or(map.entrySet().stream()
                            .filter(entry -> entry.getValue().equalsIgnoreCase(s))
                            .map(Map.Entry::getKey)::findFirst)
                    .orElse("");

            return s.chars().anyMatch(Character::isUpperCase) ? result.toUpperCase() : result;
        };

        int len = text.length();
        if (len == 1) {
            return get.apply(text);
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < len; ) {
            String c = text.substring(i, i + (i <= len - 2 ? 2 : 1));
            String leeted = get.apply(c);
            if (Strings.isEmpty(leeted)) {
                leeted = get.apply(c.charAt(0) + "");
                result.append(Strings.isEmpty(leeted) ? c.charAt(0) : leeted);
                i++;
            } else {
                result.append(leeted);
                i += 2;
            }
        }
        return result.toString();
    }

    public LeetSpeakCommand() {

        addOption(builder -> builder.name("type")
                .description("Тип перевода.")
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .required(true)
                .addChoice(ApplicationCommandOptionChoiceData.builder()
                        .name("Английский")
                        .value("en")
                        .build())
                .addChoice(ApplicationCommandOptionChoiceData.builder()
                        .name("Русский")
                        .value("ru")
                        .build()));

        addOption(builder -> builder.name("text")
                .description("Текс для перевода.")
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
                .map(str -> leeted(str, russian))
                .filter(Predicate.not(String::isBlank))
                .map(s -> MessageUtil.substringTo(s, Message.MAX_CONTENT_LENGTH))
                .map(env.event()::reply)
                .orElseGet(() -> err(env, "Не удалось перевести текст."));
    }
}
