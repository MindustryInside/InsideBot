package inside.data.api.r2dbc;

import io.r2dbc.spi.*;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Objects;

public class R2dbcConnection implements Connection {
    private final Connection delegate;

    private R2dbcConnection(Connection delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    public static R2dbcConnection of(Connection connection) {
        if (connection instanceof R2dbcConnection r) {
            return r;
        }
        return new R2dbcConnection(connection);
    }

    @Override
    public Mono<Void> beginTransaction() {
        return Mono.from(delegate.beginTransaction());
    }

    @Override
    public Mono<Void> beginTransaction(TransactionDefinition definition) {
        return Mono.from(delegate.beginTransaction(definition));
    }

    @Override
    public Mono<Void> close() {
        return Mono.from(delegate.close());
    }

    @Override
    public Mono<Void> commitTransaction() {
        return Mono.from(delegate.commitTransaction());
    }

    @Override
    public R2dbcBatch createBatch() {
        return new R2dbcBatch(delegate.createBatch());
    }

    @Override
    public Mono<Void> createSavepoint(String name) {
        return Mono.from(delegate.createSavepoint(name));
    }

    @Override
    public R2dbcStatement createStatement(String sql) {
        return R2dbcStatement.of(delegate.createStatement(sql));
    }

    @Override
    public boolean isAutoCommit() {
        return delegate.isAutoCommit();
    }

    @Override
    public ConnectionMetadata getMetadata() {
        return delegate.getMetadata();
    }

    @Override
    public IsolationLevel getTransactionIsolationLevel() {
        return delegate.getTransactionIsolationLevel();
    }

    @Override
    public Mono<Void> releaseSavepoint(String name) {
        return Mono.from(delegate.releaseSavepoint(name));
    }

    @Override
    public Mono<Void> rollbackTransaction() {
        return Mono.from(delegate.rollbackTransaction());
    }

    @Override
    public Mono<Void> rollbackTransactionToSavepoint(String name) {
        return Mono.from(delegate.rollbackTransactionToSavepoint(name));
    }

    @Override
    public Mono<Void> setAutoCommit(boolean autoCommit) {
        return Mono.from(delegate.setAutoCommit(autoCommit));
    }

    @Override
    public Mono<Void> setLockWaitTimeout(Duration timeout) {
        return Mono.from(delegate.setLockWaitTimeout(timeout));
    }

    @Override
    public Mono<Void> setStatementTimeout(Duration timeout) {
        return Mono.from(delegate.setStatementTimeout(timeout));
    }

    @Override
    public Mono<Void> setTransactionIsolationLevel(IsolationLevel isolationLevel) {
        return Mono.from(delegate.setTransactionIsolationLevel(isolationLevel));
    }

    @Override
    public Mono<Boolean> validate(ValidationDepth depth) {
        return Mono.from(delegate.validate(depth));
    }
}
