package inside.util;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.User;
import discord4j.core.object.reaction.ReactionEmoji;
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

    public static String getEmoji(ReactionEmoji emoji){
        Objects.requireNonNull(emoji, "emoji");
        if(emoji instanceof ReactionEmoji.Custom custom){
            return "<:" + custom.getName() + ":" + custom.getId().asString() + ">";
        }
        return emoji.asUnicodeEmoji().map(ReactionEmoji.Unicode::getRaw)
                .orElseThrow(IllegalStateException::new);
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

    public static String getRoleMention(Snowflake id){
        Objects.requireNonNull(id, "id");
        return "<@&" + id.asString() + ">";
    }
}
