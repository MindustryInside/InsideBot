package inside.data.api.descriptor;

import reactor.util.annotation.Nullable;

public interface JavaTypeDescriptor<T> {

    Class<?> getSqlType();

    @Nullable
    Object unwrap(T value);

    T wrap(@Nullable Object value);
}
