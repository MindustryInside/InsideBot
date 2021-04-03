package inside.util;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import org.joda.time.DateTime;
import reactor.core.Exceptions;
import reactor.util.annotation.Nullable;

import java.time.*;
import java.time.temporal.*;
import java.util.Objects;
import java.util.regex.*;

import static java.util.regex.Pattern.compile;

public abstract class MessageUtil{

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

    public static boolean isEmpty(@Nullable Message message){
        return message == null || effectiveContent(message).isEmpty();
    }

    public static String substringTo(String message, int maxLength){
        Objects.requireNonNull(message, "message");
        return message.length() >= maxLength ? message.substring(0, maxLength - 4) + "..." : message;
    }

    public static String effectiveContent(Message message){
        Objects.requireNonNull(message, "message");

        StringBuilder builder = new StringBuilder();
        if(!Strings.isEmpty(message.getContent())){
            builder.append(message.getContent());
        }

        if(!message.getAttachments().isEmpty()){
            builder.append("\n---\n");
            message.getAttachments().forEach(a -> builder.append(a.getUrl()).append("\n"));
        }
        return builder.toString();
    }

    public static boolean canParseInt(String message){
        return Strings.parseInt(message) > 0;
    }

    @Nullable
    public static Snowflake parseUserId(String message){
        try{
            return Snowflake.of(message.replaceAll("[<>@!]", ""));
        }catch(Throwable t){
            Exceptions.throwIfJvmFatal(t);
            return null;
        }
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
        return unit.addTo(instant, Strings.parseLong(amount, 0));
    }
}
