package insidebot.data.repository;

import insidebot.data.entity.MessageInfo;
import insidebot.data.repository.base.MessageRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageInfoRepository extends MessageRepository<MessageInfo>{

}
