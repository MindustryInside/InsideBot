package insidebot.util;

import discord4j.core.object.entity.*;
import reactor.util.annotation.*;

import static insidebot.InsideBot.*;

public class DiscordUtil{

    private DiscordUtil(){}

    public static boolean isBot(@Nullable User user){
        return user == null || user.isBot();
    }

    public static boolean isBot(@Nullable Member member){
        return member == null || member.isBot();
    }

    // username / membername
    public static String memberedName(@NonNull User user){
        String name = user.getUsername();
        Member member = listener.guild.getMemberById(user.getId()).block();
        if(member != null && member.getNickname().isPresent()){
            name += " / " + member.getNickname().get();
        }
        return name;
    }
}
