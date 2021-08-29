package inside.util.func;

import java.util.Objects;

@FunctionalInterface
public interface UnsafePredicate<T>{

    static <T> UnsafePredicate<T> isEqual(Object targetRef){
        return null == targetRef
                ? Objects::isNull
                : targetRef::equals;
    }

    @SuppressWarnings("unchecked")
    static <T> UnsafePredicate<T> not(UnsafePredicate<? super T> target){
        Objects.requireNonNull(target, "target");
        return (UnsafePredicate<T>)target.negate();
    }

    boolean test(T t) throws Exception;

    default UnsafePredicate<T> and(UnsafePredicate<? super T> other){
        Objects.requireNonNull(other, "other");
        return t -> test(t) && other.test(t);
    }

    default UnsafePredicate<T> negate(){
        return t -> !test(t);
    }

    default UnsafePredicate<T> or(UnsafePredicate<? super T> other){
        Objects.requireNonNull(other, "other");
        return t -> test(t) || other.test(t);
    }
}
