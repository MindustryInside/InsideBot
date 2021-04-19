package inside.service;

import discord4j.common.util.Snowflake;

public interface MessageHolderService{

    void awaitEdit(Snowflake messageId);

    void removeEdit(Snowflake messageId);

    boolean isAwaitEdit(Snowflake messageId);
}
