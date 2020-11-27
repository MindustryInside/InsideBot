package insidebot.data.repository;

import discord4j.common.util.Snowflake;
import insidebot.data.entity.MessageInfo;
import insidebot.data.repository.base.*;
import org.springframework.stereotype.Repository;
import reactor.util.annotation.NonNull;

import java.util.Optional;

@Repository
public interface MessageInfoRepository extends BaseRepository<MessageInfo, String>{

    default Optional<MessageInfo> findById(@NonNull Snowflake messageId){
        return findById(messageId.asString());
    }
}
