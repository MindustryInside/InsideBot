package inside.data.api;

import inside.util.Lazy;
import reactor.util.annotation.Nullable;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ParameterizedTypeInformation<T> extends ParentTypeAwareTypeInformation<T> {

    private final ParameterizedType type;
    private final Lazy<Boolean> resolved;

    public ParameterizedTypeInformation(ParameterizedType type, TypeDiscoverer<?> parent) {
        super(type, parent, calculateTypeVariables(type, parent));

        this.type = type;
        resolved = Lazy.of(this::isResolvedCompletely);
    }

    private static Map<TypeVariable<?>, Type> calculateTypeVariables(ParameterizedType type, TypeDiscoverer<?> parent) {
        Class<?> resolvedType = parent.resolveType(type);
        var typeParameters = resolvedType.getTypeParameters();
        var arguments = type.getActualTypeArguments();

        var localTypeVariables = new HashMap<>(parent.getTypeVariableMap());
        for (int it = 0; it < typeParameters.length; it++) {
            localTypeVariables.put(typeParameters[it], flattenTypeVariable(arguments[it], localTypeVariables));
        }

        return localTypeVariables;
    }

    private static Type flattenTypeVariable(Type source, Map<TypeVariable<?>, ? extends Type> variables) {
        if (!(source instanceof TypeVariable)) {
            return source;
        }

        Type value = variables.get(source);
        return value == null ? source : flattenTypeVariable(value, variables);
    }

    @Override
    public List<? extends TypeInformation<?>> getTypeArguments() {
        return Arrays.stream(type.getActualTypeArguments())
                .map(this::createInfo)
                .toList();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof ParameterizedTypeInformation<?> that)) {
            return false;
        }

        if (isResolved() && that.isResolved()) {
            return type.equals(that.type);
        }

        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return isResolved() ? type.hashCode() : super.hashCode();
    }

    private boolean isResolved() {
        return resolved.get();
    }

    private boolean isResolvedCompletely() {
        var typeArguments = type.getActualTypeArguments();
        if (typeArguments.length == 0) {
            return false;
        }

        for (Type typeArgument : typeArguments) {
            TypeInformation<?> info = createInfo(typeArgument);
            if (info instanceof ParameterizedTypeInformation p && !p.isResolvedCompletely()) {
                return false;
            }

            if (!(info instanceof ClassTypeInformation)) {
                return false;
            }
        }

        return true;
    }
}
