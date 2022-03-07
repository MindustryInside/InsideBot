package inside.data.api;

import inside.util.Lazy;
import reactor.util.annotation.Nullable;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.*;

class TypeDiscoverer<S> implements TypeInformation<S> {

    private final Type type;
    private final Map<TypeVariable<?>, Type> typeVariableMap;
    private final Lazy<Class<S>> resolvedType;

    protected TypeDiscoverer(Type type, Map<TypeVariable<?>, Type> typeVariableMap) {
        this.type = Objects.requireNonNull(type, "type");
        this.resolvedType = Lazy.of(() -> resolveType(type));
        this.typeVariableMap = Objects.requireNonNull(typeVariableMap, "typeVariableMap");
    }

    protected Map<TypeVariable<?>, Type> getTypeVariableMap() {
        return typeVariableMap;
    }

    protected TypeInformation<?> createInfo(Type fieldType) {
        if (fieldType.equals(type)) {
            return this;
        }

        if (fieldType instanceof Class<?> c) {
            return ClassTypeInformation.from(c);
        }

        if (fieldType instanceof ParameterizedType p) {
            return new ParameterizedTypeInformation<>(p, this);
        }

        if (fieldType instanceof TypeVariable<?> v) {
            return new TypeVariableTypeInformation<>(v, this);
        }

        if (fieldType instanceof WildcardType w) {
            Type[] bounds = w.getLowerBounds();
            if (bounds.length > 0) {
                return createInfo(bounds[0]);
            }

            bounds = w.getUpperBounds();
            if (bounds.length > 0) {
                return createInfo(bounds[0]);
            }
        }

        throw new IllegalArgumentException();
    }

    @SuppressWarnings({"unchecked"})
    protected Class<S> resolveType(Type type) {
        var map = new HashMap<>(getTypeVariableMap());
        return (Class<S>) GenericTypeResolver.resolveType(type, map);
    }

    @Override
    public Class<S> getType() {
        return resolvedType.get();
    }

    @Override
    @Nullable
    public TypeInformation<?> getSuperTypeInformation(Class<?> superType) {
        Class<?> rawType = getType();

        if (!superType.isAssignableFrom(rawType)) {
            return null;
        }

        if (getType().equals(superType)) {
            return this;
        }

        List<Type> candidates = new ArrayList<>();
        Type genericSuperclass = rawType.getGenericSuperclass();

        if (genericSuperclass != null) {
            candidates.add(genericSuperclass);
        }

        Collections.addAll(candidates, rawType.getGenericInterfaces());

        for (Type candidate : candidates) {
            TypeInformation<?> candidateInfo = createInfo(candidate);
            if (superType.equals(candidateInfo.getType())) {
                return candidateInfo;
            }

            TypeInformation<?> nestedSuperType = candidateInfo.getSuperTypeInformation(superType);
            if (nestedSuperType != null) {
                return nestedSuperType;
            }
        }

        return null;
    }

    @Override
    public List<? extends TypeInformation<?>> getTypeArguments() {
        return List.of();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TypeDiscoverer<?> that = (TypeDiscoverer<?>) o;
        return type.equals(that.type) && typeVariableMap.equals(that.typeVariableMap) && resolvedType.equals(that.resolvedType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, typeVariableMap, resolvedType);
    }
}
