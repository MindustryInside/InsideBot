package inside.command.common;

import discord4j.core.object.entity.Message;
import discord4j.core.spec.*;
import inside.command.Command;
import inside.command.model.*;
import inside.util.*;
import inside.util.io.ReusableByteInputStream;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.UnaryOperator;

import static inside.audit.BaseAuditProvider.MESSAGE_TXT;
import static inside.command.common.CommandUtils.mapOf;

@DiscordCommand(key = "translit", params = "command.transliteration.params", description = "command.transliteration.description")
public class TransliterationCommand extends Command{
    public static final Map<String, String> transliteration;

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

    public static String translit(String text){
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

    @Override
    public Publisher<?> execute(CommandEnvironment env, CommandInteraction interaction){
        String translited = interaction.getOption(0)
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asString)
                .map(TransliterationCommand::translit)
                .orElse("");

        var messageSpec = MessageCreateSpec.builder()
                .messageReference(env.message().getId());

        if(translited.isBlank()){
            messageSpec.content(messageService.get(env.context(), "message.placeholder"));
        }else if(translited.length() >= Message.MAX_CONTENT_LENGTH){
            messageSpec.addFile(MESSAGE_TXT, ReusableByteInputStream.ofString(translited));
        }else{
            messageSpec.content(translited);
        }

        return env.channel().createMessage(messageSpec.build())
                .then();
    }
}
