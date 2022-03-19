package inside.data.api;

import reactor.util.annotation.Nullable;

import java.io.Serializable;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Objects;

public class ResolvableType implements Serializable {

    public static final Type EMPTY_INSTANCE = new Type(){};
    public static final ResolvableType NONE = new ResolvableType(EMPTY_INSTANCE, null, null);

    private static final ResolvableType[] EMPTY_TYPES_ARRAY = new ResolvableType[0];

    private final Type type;
    @Nullable
    private final TypeProvider typeProvider;
    @Nullable
    private final VariableResolver variableResolver;
    @Nullable
    private final ResolvableType componentType;
    @Nullable
    private final Class<?> resolved;
    @Nullable
    private volatile ResolvableType superType;
    @Nullable
    private volatile ResolvableType[] interfaces;
    @Nullable
    private volatile ResolvableType[] generics;

    private ResolvableType(Type type, @Nullable TypeProvider typeProvider,
                           @Nullable VariableResolver variableResolver) {
        this(type, typeProvider, variableResolver, null);
    }

    private ResolvableType(Type type, @Nullable TypeProvider typeProvider,
                           @Nullable VariableResolver variableResolver,
                           @Nullable ResolvableType componentType) {

        this.type = type;
        this.typeProvider = typeProvider;
        this.variableResolver = variableResolver;
        this.componentType = componentType;
        this.resolved = resolveClass();
    }

    private ResolvableType(@Nullable Class<?> clazz) {
        this(clazz != null ? clazz : Object.class, null, null, null);
    }

    public static ResolvableType forClass(@Nullable Class<?> clazz) {
        return new ResolvableType(clazz);
    }

    public static ResolvableType forType(@Nullable Type type) {
        return forTypeResolve(type, null);
    }

    public static ResolvableType forType(@Nullable Type type, @Nullable ResolvableType owner) {
        VariableResolver variableResolver = null;
        if (owner != null) {
            variableResolver = owner.asVariableResolver();
        }
        return forTypeResolve(type, variableResolver);
    }

    static ResolvableType forTypeResolve(@Nullable Type type, @Nullable VariableResolver variableResolver) {
        if (type == null) {
            return NONE;
        }

        if (type instanceof Class<?>) {
            return new ResolvableType(type, null, variableResolver, null);
        }

        return new ResolvableType(type, null, variableResolver);
    }

    public Type getType() {
        return type;
    }

    public Class<?> toClass() {
        return resolve(Object.class);
    }

    public ResolvableType getComponentType() {
        if (equals(NONE)) {
            return NONE;
        }
        if (componentType != null) {
            return componentType;
        }
        if (type instanceof Class<?> c) {
            return forTypeResolve(c.getComponentType(), variableResolver);
        }
        if (type instanceof GenericArrayType g) {
            return forTypeResolve(g.getGenericComponentType(), variableResolver);
        }
        return resolveType().getComponentType();
    }

    public ResolvableType as(Class<?> type) {
        if (equals(NONE)) {
            return NONE;
        }
        Class<?> resolved = resolve();
        if (resolved == null || resolved == type) {
            return this;
        }
        for (ResolvableType interfaceType : getInterfaces()) {
            ResolvableType interfaceAsType = interfaceType.as(type);
            if (!interfaceAsType.equals(NONE)) {
                return interfaceAsType;
            }
        }
        return getSuperType().as(type);
    }

    public ResolvableType getSuperType() {
        Class<?> resolved = resolve();
        if (resolved == null) {
            return NONE;
        }
        try {
            Type superclass = resolved.getGenericSuperclass();
            if (superclass == null) {
                return NONE;
            }
            ResolvableType superType = this.superType;
            if (superType == null) {
                superType = forType(superclass, this);
                this.superType = superType;
            }
            return superType;
        } catch (TypeNotPresentException ex) {
            return NONE;
        }
    }

    public ResolvableType[] getInterfaces() {
        Class<?> resolved = resolve();
        if (resolved == null) {
            return EMPTY_TYPES_ARRAY;
        }
        var interfaces = this.interfaces;
        if (interfaces == null) {
            var genericIfcs = resolved.getGenericInterfaces();
            interfaces = new ResolvableType[genericIfcs.length];
            for (int i = 0; i < genericIfcs.length; i++) {
                interfaces[i] = forType(genericIfcs[i], this);
            }
            this.interfaces = interfaces;
        }
        return interfaces;
    }

    public ResolvableType getGeneric(@Nullable int... indexes) {
        var generics = getGenerics();
        if (indexes == null || indexes.length == 0) {
            return generics.length == 0 ? NONE : generics[0];
        }
        ResolvableType generic = this;
        for (int index : indexes) {
            generics = generic.getGenerics();
            if (index < 0 || index >= generics.length) {
                return NONE;
            }
            generic = generics[index];
        }
        return generic;
    }

    public ResolvableType[] getGenerics() {
        if (equals(NONE)) {
            return EMPTY_TYPES_ARRAY;
        }
        var generics = this.generics;
        if (generics == null) {
            if (type instanceof Class<?> c) {
                Type[] typeParams = c.getTypeParameters();
                generics = new ResolvableType[typeParams.length];
                for (int i = 0; i < generics.length; i++) {
                    generics[i] = forType(typeParams[i], this);
                }
            } else if (type instanceof ParameterizedType p) {
                Type[] actualTypeArguments = p.getActualTypeArguments();
                generics = new ResolvableType[actualTypeArguments.length];
                for (int i = 0; i < actualTypeArguments.length; i++) {
                    generics[i] = forTypeResolve(actualTypeArguments[i], variableResolver);
                }
            } else {
                generics = resolveType().getGenerics();
            }
            this.generics = generics;
        }
        return generics;
    }

    @Nullable
    public Class<?> resolve() {
        return resolved;
    }

    public Class<?> resolve(Class<?> fallback) {
        return resolved != null ? resolved : fallback;
    }

    @Nullable
    private Class<?> resolveClass() {
        if (type.equals(EMPTY_INSTANCE)) {
            return null;
        }
        if (type instanceof Class<?> c) {
            return c;
        }
        if (type instanceof GenericArrayType) {
            Class<?> resolvedComponent = getComponentType().resolve();
            return resolvedComponent != null ? resolvedComponent.arrayType() : null;
        }
        return resolveType().resolve();
    }

    ResolvableType resolveType() {
        if (type instanceof ParameterizedType p) {
            return forTypeResolve(p.getRawType(), variableResolver);
        }
        if (type instanceof WildcardType w) {
            Type resolved = resolveBounds(w.getUpperBounds());
            if (resolved == null) {
                resolved = resolveBounds(w.getLowerBounds());
            }
            return forTypeResolve(resolved, variableResolver);
        }
        if (type instanceof TypeVariable<?> variable) {
            if (variableResolver != null) {
                ResolvableType resolved = variableResolver.resolveVariable(variable);
                if (resolved != null) {
                    return resolved;
                }
            }
            return forTypeResolve(resolveBounds(variable.getBounds()), variableResolver);
        }
        return NONE;
    }

    @Nullable
    private Type resolveBounds(Type[] bounds) {
        if (bounds.length == 0 || bounds[0] == Object.class) {
            return null;
        }
        return bounds[0];
    }

    @Nullable
    private ResolvableType resolveVariable(TypeVariable<?> variable) {
        if (type instanceof TypeVariable) {
            return resolveType().resolveVariable(variable);
        }
        if (type instanceof ParameterizedType parameterizedType) {
            Class<?> resolved = resolve();
            if (resolved == null) {
                return null;
            }
            var variables = resolved.getTypeParameters();
            for (int i = 0; i < variables.length; i++) {
                if (Objects.deepEquals(variables[i].getName(), variable.getName())) {
                    Type actualType = parameterizedType.getActualTypeArguments()[i];
                    return forTypeResolve(actualType, variableResolver);
                }
            }
            Type ownerType = parameterizedType.getOwnerType();
            if (ownerType != null) {
                return forTypeResolve(ownerType, variableResolver).resolveVariable(variable);
            }
        }
        if (type instanceof WildcardType) {
            ResolvableType resolved = resolveType().resolveVariable(variable);
            if (resolved != null) {
                return resolved;
            }
        }
        if (variableResolver != null) {
            return variableResolver.resolveVariable(variable);
        }
        return null;
    }

    @Nullable
    VariableResolver asVariableResolver() {
        if (equals(NONE)) {
            return null;
        }

        return this::resolveVariable;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ResolvableType that = (ResolvableType) o;
        return type.equals(that.type) && Objects.equals(typeProvider, that.typeProvider) &&
                Objects.equals(variableResolver, that.variableResolver) &&
                Objects.deepEquals(componentType, that.componentType);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(type, typeProvider, variableResolver, componentType);
        result = 31 * result + Arrays.hashCode(interfaces);
        result = 31 * result + Arrays.hashCode(generics);
        return result;
    }

    interface VariableResolver {

        @Nullable
        ResolvableType resolveVariable(TypeVariable<?> variable);
    }
}
