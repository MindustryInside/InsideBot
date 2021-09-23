package inside.data.repository;

import inside.data.entity.Poll;
import inside.data.repository.base.GuildRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PollRepository extends GuildRepository<Poll>{

    Poll findByMessageId(long messageId);
}
