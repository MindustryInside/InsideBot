package inside.data.api.r2dbc;

import reactor.util.annotation.Nullable;

import java.util.function.Function;

@FunctionalInterface
public interface RowMapper<T> extends Function<R2dbcRow, T> {

    @Nullable
    @Override
    T apply(R2dbcRow r2dbcRow);
}
