package insidebot.data.repository;

import discord4j.common.util.Snowflake;
import insidebot.data.entity.MessageInfo;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageInfoRepository extends JpaRepository<MessageInfo, String>{

    boolean existsByMessageId(String messageId);

    default boolean existsByMessageId(Snowflake messageId){
        return existsById(messageId.asString());
    }

    @Query("select m from MessageInfo m where m.messageId = #{#messageId.asString()}")
    MessageInfo findByMessageId(@Param("messageId") Snowflake messageId);
}
