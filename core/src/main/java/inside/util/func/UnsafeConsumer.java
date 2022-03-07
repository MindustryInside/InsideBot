package inside.util.func;

import java.util.Objects;

@FunctionalInterface
public interface UnsafeConsumer<T> {

    void accept(T t) throws Exception;

    default UnsafeConsumer<T> andThen(UnsafeConsumer<? super T> after) {
        Objects.requireNonNull(after, "after");
        return (T t) -> {
            accept(t);
            after.accept(t);
        };
    }
}
