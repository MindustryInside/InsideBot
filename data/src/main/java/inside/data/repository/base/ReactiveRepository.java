package inside.data.repository.base;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReactiveRepository<K, T> {

    Mono<? extends T> find(K id);

    Flux<? extends T> findAll();

    Mono<? extends T> save(T entity);

    Flux<? extends T> saveAll(Iterable<? extends T> entities);

    Mono<Long> count();

    Mono<Integer> delete(T entity);
}
