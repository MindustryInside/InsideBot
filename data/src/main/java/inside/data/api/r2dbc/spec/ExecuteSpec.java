package inside.data.api.r2dbc.spec;

import inside.data.api.r2dbc.R2dbcRow;
import io.r2dbc.spi.TransactionDefinition;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.util.function.Function;

public interface ExecuteSpec {

    ExecuteSpec bind(int index, Object value);

    ExecuteSpec bindNull(int index, Class<?> type);

    ExecuteSpec bind(String name, Object value);

    ExecuteSpec bindNull(String name, Class<?> type);

    ExecuteSpec transactional(TransactionDefinition definition);

    default ExecuteSpec bindOptional(int index, @Nullable Object value) {
        if (value != null) {
            return bind(index, value);
        }
        return bindNull(index, Object.class);
    }

    default ExecuteSpec bindOptional(String name, @Nullable Object value) {
        if (value != null) {
            return bind(name, value);
        }
        return bindNull(name, Object.class);
    }

    default ExecuteSpec bindOptional(int index, @Nullable Object value, Class<?> type) {
        if (value != null) {
            return bind(index, value);
        }
        return bindNull(index, type);
    }

    default ExecuteSpec bindOptional(String name, @Nullable Object value, Class<?> type) {
        if (value != null) {
            return bind(name, value);
        }
        return bindNull(name, type);
    }

    <R> RowsFetchSpec<R> map(Function<? super R2dbcRow, ? extends R> mappingFunction);

    FetchSpec<R2dbcRow> fetch();

    Mono<Void> then();
}
