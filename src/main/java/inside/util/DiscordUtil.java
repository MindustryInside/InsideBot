package inside.util;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.User;
import reactor.util.annotation.Nullable;

import java.util.Objects;

public abstract class DiscordUtil{

    private DiscordUtil(){

    }

    public static boolean isBot(@Nullable User user){
        return user == null || user.isBot();
    }

    public static boolean isNotBot(@Nullable User user){
        return !isBot(user);
    }

    public static String getUserMention(Snowflake id){
        Objects.requireNonNull(id, "id");
        return "<@" + id.asString() + ">";
    }

    public static String getMemberMention(Snowflake id){
        Objects.requireNonNull(id, "id");
        return "<@!" + id.asString() + ">";
    }

    public static String getChannelMention(Snowflake id){
        Objects.requireNonNull(id, "id");
        return "<#" + id.asString() + ">";
    }
}
