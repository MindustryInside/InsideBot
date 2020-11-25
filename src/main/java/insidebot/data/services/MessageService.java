package insidebot.data.services;

import discord4j.common.util.Snowflake;
import insidebot.data.entity.*;
import org.springframework.lang.NonNull;

public interface MessageService{

    MessageInfo get(@NonNull MessageInfo message);

    MessageInfo getById(@NonNull String messageId);

    boolean exists(@NonNull String messageId);

    default boolean exists(@NonNull Snowflake messageId){
        return exists(messageId.asString());
    }

    default MessageInfo getById(@NonNull Snowflake messageId){
        return getById(messageId.asString());
    }

    MessageInfo save(@NonNull MessageInfo message);

    void delete(@NonNull MessageInfo message);

    void deleteById(@NonNull String memberId);

    default void deleteById(@NonNull Snowflake memberId){
        deleteById(memberId.asString());
    }
}
