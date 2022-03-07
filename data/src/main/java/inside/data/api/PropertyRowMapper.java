package inside.data.api;

import inside.data.api.r2dbc.R2dbcRow;
import inside.data.api.r2dbc.RowMapper;

import java.util.Objects;

public final class PropertyRowMapper<T> implements RowMapper<T> {
    private final Class<? extends T> type;

    private PropertyRowMapper(Class<? extends T> type) {
        this.type = Objects.requireNonNull(type, "type");
    }

    public static <T> RowMapper<T> create(Class<? extends T> type) {
        return new PropertyRowMapper<>(type);
    }

    @Override
    public T apply(R2dbcRow row) {
        return row.get(0, type);
    }
}
