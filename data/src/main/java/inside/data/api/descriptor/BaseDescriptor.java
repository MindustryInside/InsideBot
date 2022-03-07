package inside.data.api.descriptor;

public abstract class BaseDescriptor<T> implements JavaTypeDescriptor<T> {

    protected RuntimeException unknownUnwrap(Class<?> type) {
        return new IllegalArgumentException("Unknown unwrap conversion requested: " +
                getSqlType().getName() + " to " + type.getName());
    }

    protected RuntimeException unknownWrap(Class<?> type) {
        return new IllegalArgumentException("Unknown wrap conversion requested: " +
                type.getName() + " to " + getSqlType().getName());
    }
}
