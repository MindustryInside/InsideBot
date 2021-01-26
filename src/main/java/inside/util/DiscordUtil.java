package inside.util;

import discord4j.core.object.entity.*;

public abstract class DiscordUtil{

    private DiscordUtil(){}

    public static boolean isBot(User user){
        return user == null || user.isBot();
    }

    public static boolean isNotBot(User user){
        return !isBot(user);
    }

    public static String detailName(Member member){
        if(member == null) return null;
        String name = member.getUsername();
        if(member.getNickname().isPresent()){
            name += String.format(" (%s)", member.getNickname().get());
        }
        return name;
    }
}
