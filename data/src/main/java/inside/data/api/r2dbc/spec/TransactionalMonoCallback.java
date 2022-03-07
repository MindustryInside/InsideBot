package inside.data.api.r2dbc.spec;

import inside.data.api.r2dbc.R2dbcConnection;
import reactor.core.publisher.Mono;

@FunctionalInterface
public interface TransactionalMonoCallback<T> extends TransactionalCallback<T> {
    @Override
    Mono<T> execute(R2dbcConnection connection);
}
