package inside.data.repository;

import inside.data.entity.LocalMember;
import inside.data.repository.base.GuildRepository;
import org.joda.time.DateTime;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalMemberRepository extends GuildRepository<LocalMember>{

    LocalMember findByGuildIdAndUserId(String guildId, String userId);

    void deleteByLastSentMessageBefore(DateTime timestamp);
}
