package insidebot;

public class MessageUtil{

    private MessageUtil(){}

    public static String substringTo(String text, int maxLength){
        return text.length() >= maxLength ? (text.substring(0, maxLength - 4) + "...") : text;
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
