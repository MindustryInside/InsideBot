package inside.data.repository;

import inside.data.entity.MessageInfo;
import inside.data.repository.base.GuildRepository;
import org.joda.time.DateTime;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface MessageInfoRepository extends GuildRepository<MessageInfo>{

    boolean existsByMessageId(String messageId);

    MessageInfo findByMessageId(String messageId);

    @Transactional
    void deleteByTimestampBefore(DateTime timestamp);
}
