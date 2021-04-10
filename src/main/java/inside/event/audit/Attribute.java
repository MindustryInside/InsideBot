package inside.event.audit;

import discord4j.common.util.Snowflake;

public final class Attribute<T>{

    public static final Attribute<String> OLD_CONTENT = new Attribute<>("old_content");

    public static final Attribute<String> NEW_CONTENT = new Attribute<>("new_content");

    public static final Attribute<String> REASON = new Attribute<>("reason");

    public static final Attribute<Long> DELAY = new Attribute<>("delay");

    public static final Attribute<String> AVATAR_URL = new Attribute<>("avatar_url");

    public static final Attribute<Snowflake> MESSAGE_ID = new Attribute<>("message_id");

    public static final Attribute<Long> COUNT = new Attribute<>("count");

    public final String name;

    private Attribute(String name){
        this.name = name;
    }
}
