package inside.data.api;

import reactor.util.annotation.Nullable;

import java.lang.reflect.TypeVariable;
import java.util.Objects;

class TypeVariableTypeInformation<T> extends ParentTypeAwareTypeInformation<T> {

    private final TypeVariable<?> variable;

    public TypeVariableTypeInformation(TypeVariable<?> variable, TypeDiscoverer<?> parent) {
        super(variable, parent);
        this.variable = Objects.requireNonNull(variable, "variable");
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof TypeVariableTypeInformation<?> that)) return false;
        if (!super.equals(o)) return false;
        return variable.equals(that.variable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), variable);
    }
}
