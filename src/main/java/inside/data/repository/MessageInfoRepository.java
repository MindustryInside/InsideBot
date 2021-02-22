package inside.data.repository;

import inside.data.entity.MessageInfo;
import inside.data.repository.base.GuildRepository;
import org.springframework.stereotype.Repository;

import java.util.Calendar;

@Repository
public interface MessageInfoRepository extends GuildRepository<MessageInfo>{

    boolean existsByMessageId(String messageId);

    MessageInfo findByMessageId(String messageId);

    void deleteByTimestampBefore(Calendar timestamp);
}
