package insidebot.data.repository;

import discord4j.common.util.Snowflake;
import insidebot.data.entity.MessageInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageInfoRepository extends JpaRepository<MessageInfo, String>{

    boolean existsByMessageId(String messageId);

    default boolean existsByMessageId(Snowflake messageId){
        return existsById(messageId.asString());
    }

    MessageInfo findByMessageId(String messageId);

    default MessageInfo findByMessageId(Snowflake messageId){
        return findByMessageId(messageId.asString());
    }
}
