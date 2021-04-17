package inside.data.service;

import inside.data.entity.base.BaseEntity;
import reactor.core.publisher.Mono;

public interface EntityService<K, V extends BaseEntity>{

    Mono<V> find(K id);

    Mono<Void> save(V entity);

    Mono<Void> delete(K id);

    Mono<Void> delete(V entity);
}
