package inside.data.api;

import inside.data.annotation.Column;
import inside.data.annotation.Generated;
import inside.data.annotation.Id;
import inside.util.Preconditions;
import inside.util.Reflect;
import reactor.util.annotation.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public class MethodPersistentProperty implements PersistentProperty {
    private final Method method;
    @Nullable
    private final Column column;
    private final boolean id;
    private final boolean generated;

    public MethodPersistentProperty(Method method) {
        this.method = Objects.requireNonNull(method, "method");
        Preconditions.requireArgument(method.getParameterCount() == 0, () -> "Not a getter method: " + method);

        column = method.getAnnotation(Column.class);
        id = method.isAnnotationPresent(Id.class);
        generated = method.isAnnotationPresent(Generated.class);
    }

    public Method getMethod() {
        return method;
    }

    @Override
    public boolean isId() {
        return id;
    }

    @Override
    public boolean isGenerated() {
        return generated;
    }

    @Override
    public String getName() {
        return Optional.ofNullable(column)
                .map(Column::name)
                .filter(Predicate.not(String::isBlank))
                .orElseGet(method::getName);
    }

    @Override
    public Type getType() {
        return method.getGenericReturnType();
    }

    @Override
    public Optional<Column> getColumn() {
        return Optional.ofNullable(column);
    }

    @Override
    public Class<?> getClassType() {
        return method.getReturnType();
    }

    @Override
    public <T> T getValue(Object object) {
        return Reflect.invoke(method, object);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MethodPersistentProperty that = (MethodPersistentProperty) o;
        return id == that.id && generated == that.generated &&
                method.equals(that.method) && Objects.equals(column, that.column);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, column, id, generated);
    }

    @Override
    public String toString() {
        return "MethodPersistentProperty{" +
                "method=" + method +
                ", column=" + column +
                ", id=" + id +
                ", generated=" + generated +
                '}';
    }
}
