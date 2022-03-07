package inside.data.api.r2dbc;

import io.r2dbc.spi.Statement;
import reactor.core.publisher.Flux;
import reactor.util.annotation.Nullable;

import java.util.Objects;

public class R2dbcStatement implements Statement {
    private final Statement delegate;

    private R2dbcStatement(Statement delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    public static R2dbcStatement of(Statement statement) {
        if (statement instanceof R2dbcStatement r) {
            return r;
        }
        return new R2dbcStatement(statement);
    }

    @Override
    public R2dbcStatement add() {
        delegate.add();
        return this;
    }

    @Override
    public R2dbcStatement bind(int index, Object value) {
        delegate.bind(index, value);
        return this;
    }

    @Override
    public R2dbcStatement bind(String name, Object value) {
        delegate.bind(name, value);
        return this;
    }

    @Override
    public R2dbcStatement bindNull(int index, Class<?> type) {
        delegate.bindNull(index, type);
        return this;
    }

    @Override
    public R2dbcStatement bindNull(String name, Class<?> type) {
        delegate.bindNull(name, type);
        return this;
    }

    public R2dbcStatement bindOptional(int index, @Nullable Object value) {
        if (value != null) {
            return bind(index, value);
        }
        return bindNull(index, Object.class);
    }

    public R2dbcStatement bindOptional(String name, @Nullable Object value) {
        if (value != null) {
            return bind(name, value);
        }
        return bindNull(name, Object.class);
    }

    public R2dbcStatement bindOptional(int index, @Nullable Object value, Class<?> type) {
        if (value != null) {
            return bind(index, value);
        }
        return bindNull(index, type);
    }

    public R2dbcStatement bindOptional(String name, @Nullable Object value, Class<?> type) {
        if (value != null) {
            return bind(name, value);
        }
        return bindNull(name, type);
    }

    @Override
    public Flux<R2dbcResult> execute() {
        return Flux.from(delegate.execute()).map(R2dbcResult::of);
    }

    @Override
    public R2dbcStatement returnGeneratedValues(String... columns) {
        delegate.returnGeneratedValues(columns);
        return this;
    }

    @Override
    public R2dbcStatement fetchSize(int rows) {
        delegate.fetchSize(rows);
        return this;
    }
}
