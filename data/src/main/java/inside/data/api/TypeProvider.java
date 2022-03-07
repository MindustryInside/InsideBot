package inside.data.api;

import reactor.util.annotation.Nullable;

import java.lang.reflect.Type;

public interface TypeProvider {

    @Nullable
    Type getType();
}
