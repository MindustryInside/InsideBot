package inside.data.api.descriptor;

import reactor.util.annotation.Nullable;

public interface JavaTypeDescriptor<T> {

    Class<?> getSqlType();

    @Nullable
    <X> X unwrap(T value, Class<? extends X> type);

    <X> T wrap(@Nullable X value);
}
