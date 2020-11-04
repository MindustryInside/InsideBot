package insidebot;

import arc.util.Strings;

public class MessageUtil{

    private MessageUtil(){}

    public static String substringTo(String text, int maxLength){
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

    public static long parseUserId(String message){
        return Long.parseLong(message.replaceAll("[<>@!]", ""));
    }

    public static long parseRoleId(String message){
        return Long.parseLong(message.replaceAll("[<>@&]", ""));
    }

    public static long parseChannelId(String message){
        return Long.parseLong(message.replaceAll("[<>#]", ""));
    }
}
