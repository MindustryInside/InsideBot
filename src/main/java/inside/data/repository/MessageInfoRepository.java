package inside.data.repository;

import inside.data.entity.MessageInfo;
import inside.data.repository.base.GuildRepository;
import org.joda.time.DateTime;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageInfoRepository extends GuildRepository<MessageInfo>{

    boolean existsByMessageId(String messageId);

    MessageInfo findByMessageId(String messageId);

    void deleteByMessageId(String messageId);

    void deleteByTimestampBefore(DateTime timestamp);
}
