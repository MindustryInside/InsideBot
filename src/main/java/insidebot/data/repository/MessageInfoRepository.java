package insidebot.data.repository;

import discord4j.common.util.Snowflake;
import insidebot.data.entity.MessageInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import reactor.util.annotation.NonNull;

import java.util.Optional;

@Repository
public interface MessageInfoRepository extends JpaRepository<MessageInfo, String>{

    default Optional<MessageInfo> findById(@NonNull Snowflake messageId){
        return findById(messageId.asString());
    }
}
