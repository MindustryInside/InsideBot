package inside.data.api.descriptor;

import inside.data.api.PersistentProperty;
import inside.util.Reflect;
import reactor.util.annotation.Nullable;

import java.util.Objects;

public class SubEntityDescriptor extends BaseDescriptor<Object> {

    private final PersistentProperty idProperty;

    public SubEntityDescriptor(PersistentProperty idProperty) {
        this.idProperty = Objects.requireNonNull(idProperty, "idProperty");
    }

    @Override
    public Class<?> getSqlType() {
        return Reflect.wrapIfPrimitive(idProperty.getClassType());
    }

    @Nullable
    @Override
    public Object unwrap(Object value) {
        return idProperty.getValue(value);
    }

    @Override
    public Object wrap(@Nullable Object value) {
        throw new UnsupportedOperationException();
    }
}
