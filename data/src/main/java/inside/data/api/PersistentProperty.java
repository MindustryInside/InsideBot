package inside.data.api;

import inside.data.annotation.Column;
import reactor.util.annotation.Nullable;

import java.lang.reflect.Type;
import java.util.Optional;

public interface PersistentProperty {

    boolean isId();

    boolean isGenerated();

    String getName();

    Type getType();

    Optional<Column> getColumn();

    Class<?> getClassType();

    @Nullable
    <T> T getValue(Object object);
}
