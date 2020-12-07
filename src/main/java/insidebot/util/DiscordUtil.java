package insidebot.util;

import discord4j.core.object.entity.*;

public class DiscordUtil{

    private DiscordUtil(){}

    public static boolean isBot(User user){
        return user == null || user.isBot();
    }

    public static boolean isNotBot(User user){
        return !isBot(user);
    }
}
