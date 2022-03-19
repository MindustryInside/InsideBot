package inside.data.api;

import inside.data.api.r2dbc.R2dbcRow;
import inside.data.api.r2dbc.RowMapper;
import inside.util.Preconditions;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.Objects;

public class PropertiesRowMapper<T> implements RowMapper<T> {
    private final List<Class<?>> properties;

    private PropertiesRowMapper(List<Class<?>> properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    public static <T> RowMapper<T> create(List<Class<?>> properties) {
        Preconditions.requireArgument(properties.size() <= 8); // Tuple8 это лимит для нас
        return new PropertiesRowMapper<>(properties);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T apply(R2dbcRow row) {
        return switch (properties.size()) {
            case 1 -> (T) row.get(0, properties.get(0));
            default -> {
                Object[] obj = new Object[properties.size()];
                for (int i = 0, n = properties.size(); i < n; i++) {
                    obj[i] = row.getRequiredValue(i, properties.get(i));
                }

                yield (T) Tuples.fromArray(obj);
            }
        };
    }
}
