package inside.audit;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import inside.data.entity.base.NamedReference;

import java.time.Instant;
import java.util.*;

public final class Attribute<T>{

    public static final Attribute<String> OLD_CONTENT = attribute("old_content");

    public static final Attribute<String> NEW_CONTENT = attribute("new_content");

    public static final Attribute<String> REASON = attribute("reason");

    public static final Attribute<Instant> DELAY = attribute("delay");

    public static final Attribute<String> AVATAR_URL = attribute("avatar_url");

    public static final Attribute<String> OLD_AVATAR_URL = attribute("old_avatar_url");

    public static final Attribute<String> OLD_NICKNAME = attribute("old_nickname");

    public static final Attribute<String> NEW_NICKNAME = attribute("new_nickname");

    public static final Attribute<Snowflake> MESSAGE_ID = attribute("message_id");

    public static final Attribute<Long> COUNT = attribute("count");

    public static final Attribute<ReactionEmoji> REACTION_EMOJI = attribute("reaction_emoji");

    public static final Attribute<Collection<Snowflake>> ROLE_IDS = attribute("role_ids");

    public static final Attribute<NamedReference> OLD_CHANNEL = attribute("old_channel");

    public static final Attribute<Message> MESSAGE = attribute("message");

    private static <T> Attribute<T> attribute(String name){
        return new Attribute<>(name);
    }

    public final String name;

    private Attribute(String name){
        this.name = Objects.requireNonNull(name, "name");
    }

    @Override
    public boolean equals(Object o){
        if(this == o){
            return true;
        }
        if(o == null || getClass() != o.getClass()){
            return false;
        }
        Attribute<?> attribute = (Attribute<?>)o;
        return name.equals(attribute.name);
    }

    @Override
    public int hashCode(){
        return Objects.hash(name);
    }

    @Override
    public String toString(){
        return "Attribute{" + name + '}';
    }
}
