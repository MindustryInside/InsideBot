package inside.data.repository;

import inside.data.entity.Activity;
import inside.data.repository.base.GuildRepository;
import inside.data.repository.support.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ActivityRepository extends GuildRepository<Activity> {

    Mono<Activity> findByGuildIdAndUserId(long guildId, long userId);
}
