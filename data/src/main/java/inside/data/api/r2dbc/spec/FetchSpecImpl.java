package inside.data.api.r2dbc.spec;

import inside.data.api.r2dbc.R2dbcConnection;
import inside.data.api.r2dbc.R2dbcResult;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public class FetchSpecImpl<T> implements FetchSpec<T> {

    private final Mono<R2dbcConnection> connectionMono;
    private final Function<? super R2dbcConnection, ? extends Flux<R2dbcResult>> resultFunction;
    private final Function<? super R2dbcConnection, ? extends Mono<Integer>> updatedRowsFunction;
    private final BiFunction<Row, RowMetadata, ? extends T> mappingFunction;

    public FetchSpecImpl(Mono<R2dbcConnection> connectionMono,
                         Function<? super R2dbcConnection, ? extends Flux<R2dbcResult>> resultFunction,
                         Function<? super R2dbcConnection, ? extends Mono<Integer>> updatedRowsFunction,
                         BiFunction<Row, RowMetadata, ? extends T> mappingFunction) {
        this.connectionMono = Objects.requireNonNull(connectionMono, "connectionMono");
        this.resultFunction = Objects.requireNonNull(resultFunction, "resultFunction");
        this.updatedRowsFunction = Objects.requireNonNull(updatedRowsFunction, "updatedRowsFunction");
        this.mappingFunction = Objects.requireNonNull(mappingFunction, "mappingFunction");
    }

    @Override
    public Mono<T> one() {
        return all().singleOrEmpty();
    }

    @Override
    public Mono<T> first() {
        return all().next();
    }

    @Override
    public Flux<T> all() {
        return Flux.usingWhen(connectionMono,
                connection -> resultFunction.apply(connection)
                        .flatMap(result -> result.map(mappingFunction)),
                Connection::close);
    }

    @Override
    public Mono<Integer> rowsUpdated() {
        return Mono.usingWhen(connectionMono, updatedRowsFunction, Connection::close);
    }
}
