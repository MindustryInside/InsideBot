package inside.data.api;

import inside.data.api.r2dbc.R2dbcRow;
import inside.data.api.r2dbc.RowMapper;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.Objects;

public class CompositeRowMapper<T> implements RowMapper<T> {
    private final List<RowMapper<?>> mappers;

    private CompositeRowMapper(List<RowMapper<?>> mappers) {
        this.mappers = Objects.requireNonNull(mappers, "mappers");
    }

    public static <T> RowMapper<T> create(List<RowMapper<?>> mappers) {
        return new CompositeRowMapper<>(mappers);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T apply(R2dbcRow row) {
        Object[] obj = new Object[mappers.size()];
        for (int i = 0, n = mappers.size(); i < n; i++) {
            obj[i] = mappers.get(i).apply(row);
        }

        return (T) Tuples.fromArray(obj);
    }
}
