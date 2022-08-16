package inside.service.job;

import discord4j.common.util.Snowflake;
import io.netty.util.AttributeKey;

import static io.netty.util.AttributeKey.valueOf;

final class SharedAttributeKeys {

    private SharedAttributeKeys() {
    }

    static final AttributeKey<Long> ID = valueOf("id");

    static final AttributeKey<Snowflake> USER_ID = valueOf("user_id");

    static final AttributeKey<Snowflake> CHANNEL_ID = valueOf("channel_id");

    static final AttributeKey<String> MESSAGE = valueOf("message");
}
