package inside.data.repository;

import inside.data.entity.ModerationAction;
import inside.data.repository.base.GuildRepository;
import inside.data.repository.support.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ModerationActionRepository extends GuildRepository<ModerationAction> {

    Flux<ModerationAction> findAllByTypeAndGuildIdAndTargetId(ModerationAction.Type type, long guildId, long targetId);

    Mono<Long> countByTypeAndGuildIdAndTargetId(ModerationAction.Type type, long guildId, long targetId);
}
