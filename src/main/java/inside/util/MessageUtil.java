package inside.util;

import arc.util.Strings;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import org.joda.time.DateTime;
import reactor.util.annotation.Nullable;

import java.time.*;
import java.time.temporal.*;
import java.util.*;
import java.util.function.Function;
import java.util.regex.*;

import static java.util.regex.Pattern.compile;

public abstract class MessageUtil{
    private static final int DEFAULT_LEVENSHTEIN_DST = 3;

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

    public static boolean isEmpty(@Nullable CharSequence cs){
        return cs == null || cs.length() == 0;
    }

    public static boolean isEmpty(@Nullable Message message){
        return message == null || effectiveContent(message).isEmpty();
    }

    /* TODO: Change #findClosest generics from String to CharSequence for better compatibility */

    @Nullable
    public static <T> T findClosest(Iterable<? extends T> all, Function<T, String> comp, String wrong){
        return findClosest(all, comp, wrong, DEFAULT_LEVENSHTEIN_DST);
    }

    @Nullable
    public static <T> T findClosest(Iterable<? extends T> all, Function<T, String> comp, String wrong, int max){
        int min = 0;
        T closest = null;

        for(T t : all){
            int dst = Strings.levenshtein(comp.apply(t), wrong);
            if(dst < max && (closest == null || dst < min)){
                min = dst;
                closest = t;
            }
        }

        return closest;
    }

    public static String substringTo(String message, int maxLength){
        Objects.requireNonNull(message, "message");
        return message.length() >= maxLength ? message.substring(0, maxLength - 4) + "..." : message;
    }

    public static String effectiveContent(Message message){
        Objects.requireNonNull(message, "message");

        StringBuilder builder = new StringBuilder();
        if(!isEmpty(message.getContent())){
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

    public static boolean canParseId(String message){
        try{
            Snowflake.of(message);
            return true;
        }catch(Throwable t){
            return false;
        }
    }

    public static boolean canParseLong(String message){
        try{
            Long.parseLong(message);
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
        return canParseLong(amount) ? unit.addTo(instant, Long.parseLong(amount)) : instant;
    }
}
