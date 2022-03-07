package inside.data.repository;

import inside.data.entity.ReactionRole;
import inside.data.repository.base.GuildRepository;
import inside.data.repository.support.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ReactionRoleRepository extends GuildRepository<ReactionRole> {

    Mono<ReactionRole> findByGuildIdAndMessageIdAndRoleId(long guildId, long messageId, long roleId);

    Flux<ReactionRole> findAllByGuildIdAndMessageId(long guildId, long messageId);

    Mono<Long> countByGuildIdAndMessageId(long guildId, long messageId);

    Mono<Integer> deleteByGuildIdAndMessageId(long guildId, long messageId);
}
