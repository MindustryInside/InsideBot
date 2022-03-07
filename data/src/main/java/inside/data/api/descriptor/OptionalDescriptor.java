package inside.data.api.descriptor;

import reactor.util.annotation.Nullable;

import java.util.Objects;
import java.util.Optional;

public class OptionalDescriptor<T> extends BaseDescriptor<Optional<T>> {

    private final Class<T> type;

    public OptionalDescriptor(Class<T> type) {
        this.type = Objects.requireNonNull(type, "type");
    }

    @Override
    public Class<?> getSqlType() {
        return type;
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <X> X unwrap(Optional<T> value, Class<? extends X> type) {
        return (X) value.orElse(null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X> Optional<T> wrap(@Nullable X value) {
        if (value == null) {
            return Optional.empty();
        }

        if (type.isInstance(value)) {
            return Optional.of((T) value);
        }

        throw unknownWrap(value.getClass());
    }
}
