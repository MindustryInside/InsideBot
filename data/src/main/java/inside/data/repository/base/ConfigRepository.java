package inside.data.repository.base;

import inside.data.entity.base.ConfigEntity;
import reactor.core.publisher.Flux;

public interface ConfigRepository<E extends ConfigEntity> extends GuildRepository<E> {

    Flux<E> findAllEnabled(boolean state);
}
