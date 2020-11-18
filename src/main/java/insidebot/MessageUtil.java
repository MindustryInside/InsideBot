package insidebot;

import arc.util.Strings;
import discord4j.common.util.Snowflake;
import reactor.util.annotation.*;

import java.util.Objects;

public class MessageUtil{

    private MessageUtil(){}

    public static String substringTo(@NonNull String text, int maxLength){
        Objects.requireNonNull(text, "Message must not be null."); // на всякий, если как-то *удастся* нарушить контракт
        return text.length() >= maxLength ? (text.substring(0, maxLength - 4) + "...") : text;
    }

    public static boolean canParseInt(String message){
        return Strings.canParseInt(message) && Strings.parseInt(message) > 0;
    }

    /* Сообщение может быть пустым, и из-за этого вылетит ошибка */
    public static Snowflake parseUserId(@NonNull String message){
        Objects.requireNonNull(message, "Message must not be null.");
        return Snowflake.of(message.replaceAll("[<>@!]", ""));
    }

    public static Snowflake parseRoleId(@NonNull String message){
        Objects.requireNonNull(message, "Message must not be null.");
        return Snowflake.of(message.replaceAll("[<>@&]", ""));
    }

    public static Snowflake parseChannelId(@NonNull String message){
        Objects.requireNonNull(message, "Message must not be null.");
        return Snowflake.of(message.replaceAll("[<>#]", ""));
    }
}
