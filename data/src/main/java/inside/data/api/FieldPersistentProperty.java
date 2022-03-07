package inside.data.api;

import inside.data.annotation.Column;
import inside.data.annotation.Generated;
import inside.data.annotation.Id;
import inside.util.Reflect;
import reactor.util.annotation.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public class FieldPersistentProperty implements PersistentProperty {
    private final Field field;
    private final boolean id;
    private final boolean generated;
    @Nullable
    private final Column column;

    public FieldPersistentProperty(Field field) {
        this.field = Objects.requireNonNull(field, "field");

        column = field.getAnnotation(Column.class);
        id = field.isAnnotationPresent(Id.class);
        generated = field.isAnnotationPresent(Generated.class);
    }

    public Field getField() {
        return field;
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
                .orElseGet(field::getName);
    }

    @Override
    public Type getType() {
        return field.getGenericType();
    }

    @Override
    public Optional<Column> getColumn() {
        return Optional.ofNullable(column);
    }

    @Override
    public Class<?> getClassType() {
        return field.getType();
    }

    @Override
    public <T> T getValue(Object object) {
        return Reflect.get(field, object);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FieldPersistentProperty that = (FieldPersistentProperty) o;
        return id == that.id && generated == that.generated &&
                field.equals(that.field) &&
                Objects.equals(column, that.column);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, id, generated, column);
    }

    @Override
    public String toString() {
        return "FieldPersistentProperty{" +
                "field=" + field +
                ", id=" + id +
                ", generated=" + generated +
                ", column=" + column +
                '}';
    }
}
