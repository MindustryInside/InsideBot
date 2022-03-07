package inside.data.repository;

import inside.data.entity.Starboard;
import inside.data.repository.base.GuildRepository;
import inside.data.repository.support.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface StarboardRepository extends GuildRepository<Starboard> {

    Mono<Starboard> findByGuildIdAndSourceMessageId(long guildId, long sourceMessageId);

    Mono<Integer> deleteByGuildIdAndSourceMessageId(long guildId, long sourceMessageId);
}
