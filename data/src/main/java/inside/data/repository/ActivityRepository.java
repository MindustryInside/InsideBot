package inside.data.repository;

import inside.data.annotation.Query;
import inside.data.entity.Activity;
import inside.data.entity.ImmutableActivity;
import inside.data.repository.base.GuildRepository;
import inside.data.repository.support.Repository;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@Repository
public interface ActivityRepository extends GuildRepository<Activity> {

    Mono<Activity> findByGuildIdAndUserId(long guildId, long userId);

    @Query("""
            select *
            from (select row_number() over (order by message_count desc, last_sent_message desc) as position, * from activity where guild_id = $1) result
            where user_id = $2""")
    Mono<Tuple2<Long, ImmutableActivity>> findActivityPositionById(long guildId, long userId);
}
