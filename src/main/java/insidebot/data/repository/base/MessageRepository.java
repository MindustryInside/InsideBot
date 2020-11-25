package insidebot.data.repository.base;

import discord4j.common.util.Snowflake;
import insidebot.data.entity.MessageInfo;
import reactor.util.annotation.NonNull;

public interface MessageRepository<T extends MessageInfo> extends BaseRepository<T, String>{
    T findByMessageId(@NonNull String messageId);

    default T findByMessageId(@NonNull Snowflake messageId){
        return findByMessageId(messageId.asString());
    }
}
