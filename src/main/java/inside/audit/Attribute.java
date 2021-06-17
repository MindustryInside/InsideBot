package inside.audit;

import discord4j.common.util.Snowflake;
import discord4j.core.object.reaction.ReactionEmoji;

public final class Attribute<T>{

    public static final Attribute<String> OLD_CONTENT = new Attribute<>("old_content");

    public static final Attribute<String> NEW_CONTENT = new Attribute<>("new_content");

    public static final Attribute<String> REASON = new Attribute<>("reason");

    public static final Attribute<Long> DELAY = new Attribute<>("delay");

    public static final Attribute<String> AVATAR_URL = new Attribute<>("avatar_url");

    public static final Attribute<String> OLD_AVATAR_URL = new Attribute<>("old_avatar_url");

    public static final Attribute<Snowflake> MESSAGE_ID = new Attribute<>("message_id");

    public static final Attribute<Long> COUNT = new Attribute<>("count");

    public static final Attribute<ReactionEmoji> REACTION_EMOJI = new Attribute<>("reaction_emoji");

    public static final Attribute<Snowflake> ROLE_ID = new Attribute<>("role_id");

    public final String name;

    private Attribute(String name){
        this.name = name;
    }
}
