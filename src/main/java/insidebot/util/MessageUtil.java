package insidebot.util;

import arc.util.Strings;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import reactor.util.annotation.*;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class MessageUtil{

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
            message.getAttachments().forEach(a -> builder.append(a.getUrl()).append("\n"));
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

    public static String zonedFormat(){
        return DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss ZZZZ").format(ZonedDateTime.now());
    }

    public static DateTimeFormatter dateTime(){
        return DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");
    }
}
