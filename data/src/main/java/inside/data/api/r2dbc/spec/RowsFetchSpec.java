package inside.data.api.r2dbc.spec;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RowsFetchSpec<T> {

    Mono<T> one();

    Mono<T> first();

    Flux<T> all();
}
