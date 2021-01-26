package inside.data.repository;

import discord4j.common.util.Snowflake;
import inside.data.entity.MessageInfo;
import inside.data.repository.base.GuildRepository;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageInfoRepository extends GuildRepository<MessageInfo>{

    boolean existsByMessageId(String messageId);

    default boolean existsByMessageId(Snowflake messageId){
        return existsByMessageId(messageId.asString());
    }

    @Query("select m from MessageInfo m where m.messageId = :#{#messageId?.asString()}")
    MessageInfo findByMessageId(Snowflake messageId);
}
