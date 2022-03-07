package inside.data.api.r2dbc;

import discord4j.discordjson.possible.Possible;
import inside.data.api.r2dbc.spec.*;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public interface DatabaseClient extends ConnectionFactory {

    default ExecuteSpec sql(String sql) {
        return sql(() -> sql);
    }

    ExecuteSpec sql(Supplier<String> sqlSupplier);

    default ExecuteSpec sqlWithConnection(String sql, Connection connection) {
        return sqlWithConnection(() -> sql, connection);
    }

    ExecuteSpec sqlWithConnection(Supplier<String> sqlSupplier, Connection connection);

    @Override
    Mono<R2dbcConnection> create();

    default <T> TransactionalFlux<T> transactionalMany(TransactionalFluxCallback<T> callback) {
        return TransactionalFlux.of(Possible.absent(), create(), callback);
    }

    default <T> TransactionalFlux<T> transactionalMany(Connection connection, TransactionalFluxCallback<T> callback) {
        return TransactionalFlux.of(Possible.absent(), Mono.fromSupplier(() -> R2dbcConnection.of(connection)), callback);
    }

    default <T> TransactionalMono<T> transactional(TransactionalMonoCallback<T> callback) {
        return TransactionalMono.of(Possible.absent(), create(), callback);
    }

    default <T> TransactionalMono<T> transactional(Connection connection, TransactionalMonoCallback<T> callback) {
        return TransactionalMono.of(Possible.absent(), Mono.fromSupplier(() -> R2dbcConnection.of(connection)), callback);
    }

    default <T> Mono<T> inConnection(Function<? super R2dbcConnection, ? extends Mono<T>> action) {
        Objects.requireNonNull(action, "action");

        return Mono.usingWhen(create(), action, Connection::close);
    }

    default <T> Flux<T> inConnectionMany(Function<? super R2dbcConnection, ? extends Flux<T>> action) {
        Objects.requireNonNull(action, "action");

        return Flux.usingWhen(create(), action, Connection::close);
    }
}
