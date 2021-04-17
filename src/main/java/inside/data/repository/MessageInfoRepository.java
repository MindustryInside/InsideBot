package inside.data.repository;

import inside.data.entity.MessageInfo;
import inside.data.repository.base.GuildRepository;
import org.joda.time.DateTime;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageInfoRepository extends GuildRepository<MessageInfo>{

    MessageInfo findByMessageId(long messageId);

    void deleteByMessageId(long messageId);

    void deleteByTimestampBefore(DateTime timestamp);
}
