package insidebot.util;

import discord4j.core.object.entity.*;
import reactor.util.annotation.Nullable;

public class DiscordUtil{

    private DiscordUtil(){}

    public static boolean isBot(@Nullable User user){
        return user == null || user.isBot();
    }

    public static boolean isBot(@Nullable Member member){
        return member == null || member.isBot();
    }
}
