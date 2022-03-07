package inside.data.api;

import reactor.util.annotation.Nullable;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import java.util.Objects;

public abstract class ParentTypeAwareTypeInformation<S> extends TypeDiscoverer<S> {

    private final TypeDiscoverer<?> parent;

    protected ParentTypeAwareTypeInformation(Type type, TypeDiscoverer<?> parent) {
        this(type, parent, parent.getTypeVariableMap());
    }

    protected ParentTypeAwareTypeInformation(Type type, TypeDiscoverer<?> parent, Map<TypeVariable<?>, Type> map) {
        super(type, map);
        this.parent = parent;
    }

    @Override
    protected TypeInformation<?> createInfo(Type fieldType) {
        if (parent.getType().equals(fieldType)) {
            return parent;
        }

        return super.createInfo(fieldType);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        ParentTypeAwareTypeInformation<?> that = (ParentTypeAwareTypeInformation<?>) o;
        return parent.equals(that.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), parent);
    }
}
