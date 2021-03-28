package inside.service;

import discord4j.common.util.Snowflake;
import inside.data.entity.MessageInfo;
import reactor.util.annotation.Nullable;

public interface MessageHolderService{

    void awaitEdit(Snowflake messageId);

    void removeEdit(Snowflake messageId);

    boolean isAwaitEdit(Snowflake messageId);

    @Nullable
    MessageInfo getById(Snowflake messageId);

    void deleteById(Snowflake messageId);

    void delete(MessageInfo message);

    void save(MessageInfo message);

    void cleanUp();
}
