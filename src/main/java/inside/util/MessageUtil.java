package inside.util;

import arc.util.Strings;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import org.joda.time.DateTime;
import reactor.util.annotation.*;

import java.time.*;
import java.time.temporal.*;
import java.util.regex.*;

import static java.util.regex.Pattern.compile;

public class MessageUtil{

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

    public static boolean isEmpty(CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    public static boolean isEmpty(Message message) {
        return message == null || effectiveContent(message).isEmpty();
    }

    public static String substringTo(@NonNull String text, int maxLength){
        return text.length() >= maxLength ? (text.substring(0, maxLength - 4) + "...") : text;
    }

    public static String effectiveContent(@NonNull Message message){
        StringBuilder builder = new StringBuilder(message.getContent());
        if(!message.getAttachments().isEmpty()){
            builder.append("\n---\n");
            message.getAttachments().forEach(a -> builder.append(a.getUrl()).append('\n'));
        }
        return builder.toString();
    }

    public static boolean canParseInt(String message){
        return Strings.canParseInt(message) && Strings.parseInt(message) > 0;
    }

    public static boolean canParseId(String message){
        try{
            Snowflake.of(message);
            return true;
        }catch(Exception e){
            return false;
        }
    }

    public static Snowflake parseUserId(@NonNull String message){
        message = message.replaceAll("[<>@!]", "");
        return canParseId(message) ? Snowflake.of(message) : null;
    }

    public static Snowflake parseRoleId(@NonNull String message){
        message = message.replaceAll("[<>@&]", "");
        return canParseId(message) ? Snowflake.of(message) : null;
    }

    public static Snowflake parseChannelId(@NonNull String message){
        message = message.replaceAll("[<>#]", "");
        return canParseId(message) ? Snowflake.of(message) : null;
    }

    public static DateTime parseTime(String message){
        if(message == null) return null;
        Matcher matcher = timeUnitPattern.matcher(message.toLowerCase());
        if(!matcher.matches()) return null;
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
