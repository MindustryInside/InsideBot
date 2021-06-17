package inside.util.func;

@FunctionalInterface
public interface BooleanFunction<T>{

    T apply(boolean value);
}
