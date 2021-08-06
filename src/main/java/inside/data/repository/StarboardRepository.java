package inside.data.repository;

import inside.data.entity.Starboard;
import inside.data.repository.base.GuildRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StarboardRepository extends GuildRepository<Starboard>{

    Starboard findByGuildIdAndSourceMessageId(long guildId, long sourceMessageId);

    Starboard findByGuildIdAndTargetMessageId(long guildId, long targetMessageId);
}
