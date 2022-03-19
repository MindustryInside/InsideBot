package inside.data.api.r2dbc;

import inside.data.api.r2dbc.spec.ExecuteSpec;
import inside.data.api.r2dbc.spec.ExecuteSpecImpl;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.function.Supplier;

public final class DefaultDatabaseClient implements DatabaseClient {
    private final ConnectionFactory delegate;

    public DefaultDatabaseClient(ConnectionFactory delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public ExecuteSpec sql(Supplier<String> sqlSupplier) {
        return new ExecuteSpecImpl(this, sqlSupplier);
    }

    @Override
    public ExecuteSpec sqlWithConnection(Supplier<String> sqlSupplier, Connection connection) {
        return new ExecuteSpecImpl(this, sqlSupplier, R2dbcConnection.of(connection));
    }

    @Override
    public Mono<R2dbcConnection> create() {
        return Mono.from(delegate.create()).map(R2dbcConnection::of);
    }

    @Override
    public ConnectionFactoryMetadata getMetadata() {
        return delegate.getMetadata();
    }
}
