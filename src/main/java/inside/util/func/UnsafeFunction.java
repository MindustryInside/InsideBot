package inside.util.func;

import java.util.Objects;

@FunctionalInterface
public interface UnsafeFunction<T, R>{

    static <T> UnsafeFunction<T, T> identity(){
        return t -> t;
    }

    R apply(T t) throws Exception;

    default <V> UnsafeFunction<V, R> compose(UnsafeFunction<? super V, ? extends T> before){
        Objects.requireNonNull(before, "before");
        return (V v) -> apply(before.apply(v));
    }

    default <V> UnsafeFunction<T, V> andThen(UnsafeFunction<? super R, ? extends V> after){
        Objects.requireNonNull(after, "after");
        return (T t) -> after.apply(apply(t));
    }
}
