package inside.data.api.r2dbc.spec;

import discord4j.discordjson.possible.Possible;
import inside.data.api.r2dbc.R2dbcConnection;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.TransactionDefinition;
import org.immutables.value.Value;
import reactor.core.CorePublisher;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

interface BaseTransactionalPublisher<T> extends CorePublisher<T> {

    TransactionalCallback<T> callback();

    Possible<TransactionDefinition> definition();

    Mono<R2dbcConnection> connection();

    @Override
    String toString();
}

@Value.Immutable(builder = false)
abstract class TransactionalMonoGenerator<T> extends Mono<T>
        implements BaseTransactionalPublisher<T> {
    @Override
    public abstract TransactionalMonoCallback<T> callback();

    @Override
    public abstract String toString();

    @Override
    public void subscribe(CoreSubscriber<? super T> actual) {
        Mono.usingWhen(connection(),
                        connection -> {
                            Mono<Void> beginTransaction = !definition().isAbsent()
                                    ? connection.beginTransaction(definition().get())
                                    : connection.beginTransaction();

                            return Mono.usingWhen(beginTransaction.thenReturn(connection),
                                    ignored -> Mono.from(callback().execute(connection)),
                                    ignored -> connection.commitTransaction(),
                                    (ignored, t) -> connection.rollbackTransaction().then(Mono.error(t)),
                                    ignored -> connection.rollbackTransaction());
                        },
                        Connection::close)
                .subscribe(actual);
    }
}

@Value.Immutable(builder = false)
abstract class TransactionalFluxGenerator<T> extends Flux<T>
        implements BaseTransactionalPublisher<T> {

    @Override
    public abstract TransactionalFluxCallback<T> callback();

    @Override
    public abstract String toString();

    @Override
    public void subscribe(CoreSubscriber<? super T> actual) {
        Flux.usingWhen(connection(),
                        connection -> {
                            Mono<Void> beginTransaction = !definition().isAbsent()
                                    ? connection.beginTransaction(definition().get())
                                    : connection.beginTransaction();

                            return Flux.usingWhen(beginTransaction.thenReturn(connection),
                                    ignored -> Flux.from(callback().execute(connection)),
                                    ignored -> connection.commitTransaction(),
                                    (ignored, t) -> connection.rollbackTransaction().then(Mono.error(t)),
                                    ignored -> connection.rollbackTransaction());
                        },
                        Connection::close)
                .subscribe(actual);
    }
}
