package inside.command.common;

import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageCreateSpec;
import inside.command.Command;
import inside.command.model.*;
import inside.util.Strings;
import inside.util.io.ReusableByteInputStream;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.UnaryOperator;

import static inside.audit.BaseAuditProvider.MESSAGE_TXT;

@DiscordCommand(key = {"leet", "1337"}, params = "command.1337.params", description = "command.1337.description")
public class LeetSpeakCommand extends Command{
    public static final Map<String, String> rusLeetSpeak;
    public static final Map<String, String> engLeetSpeak;

    static{
        rusLeetSpeak = CommandUtils.mapOf(
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

        engLeetSpeak = CommandUtils.mapOf(
                "a", "4", "b", "8", "c", "c", "d", "d",
                "e", "3", "f", "ph", "g", "9", "h", "h",
                "i", "1", "j", "g", "k", "k", "l", "l",
                "m", "m", "n", "n", "o", "0", "p", "p",
                "q", "q", "r", "r", "s", "5", "t", "7",
                "u", "u", "v", "v", "w", "w", "x", "x",
                "y", "y", "z", "2"
        );
    }

    public static String leeted(String text, boolean russian){
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
        if(len == 1){
            return get.apply(text);
        }

        StringBuilder result = new StringBuilder();
        for(int i = 0; i < len; ){
            String c = text.substring(i, i + (i <= len - 2 ? 2 : 1));
            String leeted = get.apply(c);
            if(Strings.isEmpty(leeted)){
                leeted = get.apply(c.charAt(0) + "");
                result.append(Strings.isEmpty(leeted) ? c.charAt(0) : leeted);
                i++;
            }else{
                result.append(leeted);
                i += 2;
            }
        }
        return result.toString();
    }

    @Override
    public Mono<Void> execute(CommandEnvironment env, CommandInteraction interaction){
        boolean ru = interaction.getOption(0)
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asString)
                .map("ru"::equalsIgnoreCase)
                .orElse(false);

        String text = interaction.getOption(1)
                .flatMap(CommandOption::getValue)
                .map(OptionValue::asString)
                .map(str -> leeted(str, ru))
                .orElse("");

        var messageSpec = MessageCreateSpec.builder()
                .messageReference(env.message().getId());

        if(text.isBlank()){
            messageSpec.content(messageService.get(env.context(), "message.placeholder"));
        }else if(text.length() >= Message.MAX_CONTENT_LENGTH){
            messageSpec.addFile(MESSAGE_TXT, ReusableByteInputStream.ofString(text));
        }else{
            messageSpec.content(text);
        }

        return env.channel().createMessage(messageSpec.build())
                .then();
    }
}
