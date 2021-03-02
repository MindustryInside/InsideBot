package inside.util;

import arc.struct.StringMap;
import arc.util.Strings;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import org.joda.time.*;
import reactor.util.annotation.Nullable;

import java.time.LocalDateTime;
import java.time.*;
import java.time.temporal.*;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.regex.*;

import static java.util.regex.Pattern.compile;

public abstract class MessageUtil{

    private static final String[] latPattern;
    private static final String[] rusPattern;

    static{
        String lat = "Q-W-E-R-T-Y-U-I-O-P-A-S-D-F-G-H-J-K-L-Z-X-C-V-B-N-M";
        String rus = "Й-Ц-У-К-Е-Н-Г-Ш-Щ-З-Ф-Ы-В-А-П-Р-О-Л-Д-Я-Ч-С-М-И-Т-Ь";
        latPattern = (lat + "-" + lat.toLowerCase() + "-\\^-:-\\$-@-&-~-`-\\{-\\[-\\}-\\]-\"-'-<->-;-\\?-\\/-\\.-,-#").split("-");
        rusPattern = (rus + "-" + rus.toLowerCase() + "-:-Ж-;-\"-\\?-Ё-ё-Х-х-Ъ-ъ-Э-э-Б-Ю-ж-,-\\.-ю-б-№").split("-");
    }

    public static StringMap leetSpeak = StringMap.of(
            "а", "4", "a", "4",
            "б", "6", "b", "8",
            "в", "8", "c", "c",
            "г", "g", "d", "d",
            "д", "d", "e", "3",
            "е", "3", "f", "ph",
            "ё", "3", "g", "9",
            "ж", "zh", "h", "h",
            "з", "e", "i", "1",
            "и", "i", "j", "g",
            "й", "\\`i", "k", "k",
            "к", "k", "l", "l",
            "л", "l", "m", "m",
            "м", "m", "n", "n",
            "н", "n", "o", "0",
            "о", "0", "p", "p",
            "п", "p", "q", "q",
            "р", "r", "r", "r",
            "с", "c", "s", "5",
            "т", "7", "t", "7",
            "у", "y", "u", "u",
            "ф", "f", "v", "v",
            "х", "x", "w", "w",
            "ц", "u,", "x", "x",
            "ч", "ch", "y", "y",
            "ш", "w", "z", "2",
            "щ", "w,",
            "ъ", "\\`ь",
            "ы", "ьi",
            "ь", "ь",
            "э", "э",
            "ю", "10",
            "я", "9"
    );

    public static StringMap translit = StringMap.of(
            "a", "а",
            "b", "б",
            "v", "в",
            "g", "г",
            "d", "д",
            "e", "е",
            "yo", "ё",
            "zh", "ж",
            "z", "з",
            "i", "и",
            "j", "й",
            "k", "к",
            "l", "л",
            "m", "м",
            "n", "н",
            "o", "о",
            "p", "п",
            "r", "р",
            "s", "с",
            "t", "т",
            "u", "у",
            "f", "ф",
            "h", "х",
            "ts", "ц",
            "ch", "ч",
            "sh", "ш",
            "\\`", "ъ",
            "y", "у",
            "'", "ь",
            "yu", "ю",
            "ya", "я",
            "x", "кс",
            "v", "в",
            "q", "к",
            "iy", "ий"
    );

    private static final Pattern timeUnitPattern = compile(
            "^" +
            "((\\d+)(y|year|years|г|год|года|лет))?" +
            "((\\d+)(m|mon|month|months|мес|месяц|месяца|месяцев))?" +
            "((\\d+)(w|week|weeks|н|нед|неделя|недели|недель|неделю))?" +
            "((\\d+)(d|day|days|д|день|дня|дней))?" +
            "((\\d+)(h|hour|hours|ч|час|часа|часов))?" +
            "((\\d+)(min|mins|minute|minutes|мин|минута|минуту|минуты|минут))?" +
            "((\\d+)(s|sec|secs|second|seconds|с|c|сек|секунда|секунду|секунды|секунд))?$"
    );

    private MessageUtil(){}

    public static boolean isNotEmpty(CharSequence cs){
        return !isEmpty(cs);
    }

    public static boolean isEmpty(@Nullable CharSequence cs){
        return cs == null || cs.length() == 0;
    }

    public static boolean isEmpty(@Nullable Message message){
        return message == null || effectiveContent(message).isBlank();
    }

    public static String text2rus(String text){
        for(int i = 0; i < latPattern.length; i++){
            text = text.replaceAll("(?u)" + latPattern[i], rusPattern[i]);
        }
        return text;
    }

    public static String text2lat(String text){
        for(int i = 0; i < latPattern.length; i++){
            text = text.replaceAll("(?u)" + rusPattern[i], latPattern[i]);
        }
        return text;
    }

    public static String leeted(String text){
        UnaryOperator<String> get = s -> {
            String result = leetSpeak.get(s.toLowerCase());
            if(result == null){
                result = leetSpeak.findKey(s.toLowerCase(), false);
            }
            return result != null ? Character.isUpperCase(s.charAt(0)) ? Character.toUpperCase(result.charAt(0)) +
                    (result.length() > 1 ? result.substring(1).toUpperCase() : "") : result : "";
        };

        int len = text.length();
        if(len == 0){
            return "";
        }
        if(len == 1){
            return get.apply(text);
        }

        StringBuilder result = new StringBuilder();
        for(int i = 0; i < len; ){
            String c = text.substring(i, i <= len - 2 ? i + 2 : i + 1);
            String leeted = get.apply(c);
            if(isEmpty(leeted)){
                leeted = get.apply(c.charAt(0) + "");
                result.append(isEmpty(leeted) ? c.charAt(0) : leeted);
                i++;
            }else{
                result.append(leeted);
                i += 2;
            }
        }
        return result.toString();
    }

    public static String translit(String text){
        UnaryOperator<String> get = s -> {
            String result = translit.get(s.toLowerCase());
            if(result == null){
                result = translit.findKey(s.toLowerCase(), false);
            }
            return result != null ? Character.isUpperCase(s.charAt(0)) ? Character.toUpperCase(result.charAt(0)) +
                    (result.length() > 1 ? result.substring(1).toUpperCase() : "") : result : "";
        };

        int len = text.length();
        if(len == 0){
            return "";
        }
        if(len == 1){
            return get.apply(text);
        }

        StringBuilder result = new StringBuilder();
        for(int i = 0; i < len; ){
            String c = text.substring(i, i <= len - 2 ? i + 2 : i + 1);
            String translited = get.apply(c);
            if(isEmpty(translited)){
                translited = get.apply(c.charAt(0) + "");
                result.append(isEmpty(translited) ? c.charAt(0) : translited);
                i++;
            }else{
                result.append(translited);
                i += 2;
            }
        }
        return result.toString();
    }

    public static String substringTo(String message, int maxLength){
        Objects.requireNonNull(message, "message");
        return message.length() >= maxLength ? message.substring(0, maxLength - 4) + "..." : message;
    }

    public static String effectiveContent(Message message){
        Objects.requireNonNull(message, "message");
        if(isEmpty(message.getContent())){
            return "";
        }

        StringBuilder builder = new StringBuilder(message.getContent());
        if(!message.getAttachments().isEmpty()){
            builder.append("\n---\n");
            message.getAttachments().forEach(a -> builder.append(a.getUrl()).append("\n"));
        }
        return builder.toString();
    }

    @Nullable
    public static DateTimeZone find(String id){
        try{
            return DateTimeZone.forID(id);
        }catch(Throwable t){
            return null;
        }
    }

    public static boolean canParseInt(String message){
        return Strings.canParseInt(message) && Strings.parseInt(message) > 0;
    }

    public static boolean canParseId(String message){
        try{
            Snowflake.of(message);
            return true;
        }catch(Throwable t){
            return false;
        }
    }

    @Nullable
    public static Snowflake parseUserId(String message){
        message = message.replaceAll("[<>@!]", "");
        return canParseId(message) ? Snowflake.of(message) : null;
    }

    @Nullable
    public static DateTime parseTime(String message){
        Matcher matcher = timeUnitPattern.matcher(message.toLowerCase());
        if(!matcher.matches()){
            return null;
        }

        LocalDateTime offsetDateTime = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);
        offsetDateTime = addUnit(offsetDateTime, ChronoUnit.YEARS, matcher.group(2));
        offsetDateTime = addUnit(offsetDateTime, ChronoUnit.MONTHS, matcher.group(5));
        offsetDateTime = addUnit(offsetDateTime, ChronoUnit.WEEKS, matcher.group(8));
        offsetDateTime = addUnit(offsetDateTime, ChronoUnit.DAYS, matcher.group(11));
        offsetDateTime = addUnit(offsetDateTime, ChronoUnit.HOURS, matcher.group(14));
        offsetDateTime = addUnit(offsetDateTime, ChronoUnit.MINUTES, matcher.group(17));
        offsetDateTime = addUnit(offsetDateTime, ChronoUnit.SECONDS, matcher.group(20));
        return DateTime.now().plus(offsetDateTime.toEpochSecond(ZoneOffset.UTC) * 1000);
    }

    private static <T extends Temporal> T addUnit(T instant, ChronoUnit unit, String amount){
        return Strings.canParseInt(amount) ? unit.addTo(instant, Long.parseLong(amount)) : instant;
    }
}
