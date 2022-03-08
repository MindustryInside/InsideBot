package inside.data.repository;

import inside.data.annotation.Query;
import inside.data.entity.Activity;
import inside.data.repository.base.GuildRepository;
import inside.data.repository.support.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ActivityRepository extends GuildRepository<Activity> {

    Mono<Activity> findByGuildIdAndUserId(long guildId, long userId);

    @Query("""
            select position
            from (select *, row_number() over (order by message_count desc, last_sent_message desc) as position from activity) result
            where guild_id = $1 and user_id = $2;""")
    Mono<Long> findActivityPositionById(long guildId, long userId);
}
