package inside.data.repository.base;

import discord4j.common.util.Snowflake;
import inside.data.entity.base.GuildEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface GuildRepository<E extends GuildEntity> extends ReactiveRepository<Long, E> {

    Mono<E> findByGuildId(long guildId);

    Flux<E> findAllByGuildId(long guildId);

    Mono<Long> countByGuildId(long guildId);

    Mono<Void> deleteByGuildId(Snowflake guildId);
}
