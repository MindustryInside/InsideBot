package insidebot;

import arc.util.Strings;
import discord4j.common.util.Snowflake;
import reactor.util.annotation.NonNull;

import java.util.Objects;

public class MessageUtil{

    private MessageUtil(){}

    public static String substringTo(@NonNull String text, int maxLength){
        Objects.requireNonNull(text, "Message must not be null.");
        return text.length() >= maxLength ? (text.substring(0, maxLength - 4) + "...") : text;
    }

    // Только позитивные числа, 0 не примет
    public static boolean canParseInt(String message){
        try{
            return Strings.parseInt(message) > 0;
        }catch(Exception e){
            return false;
        }
    }

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
