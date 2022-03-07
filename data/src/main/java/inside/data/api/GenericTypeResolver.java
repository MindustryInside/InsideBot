package inside.data.api;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class GenericTypeResolver {

    private static final ConcurrentMap<Class<?>, Map<TypeVariable<?>, Type>> typeVariableCache = new ConcurrentHashMap<>();

    private GenericTypeResolver() {
    }

    public static Class<?> resolveType(Type genericType, Map<TypeVariable<?>, ? extends Type> map) {
        ResolvableType.VariableResolver res = var -> {
            Type type = map.get(var);
            return type != null ? ResolvableType.forType(type) : null;
        };

        return ResolvableType.forTypeResolve(genericType, res).toClass();
    }

    public static Map<TypeVariable<?>, Type> getTypeVariableMap(Class<?> clazz) {
        var typeVariableMap = typeVariableCache.get(clazz);
        if (typeVariableMap == null) {
            typeVariableMap = new HashMap<>();
            buildTypeVariableMap(ResolvableType.forClass(clazz), typeVariableMap);
            typeVariableCache.put(clazz, Collections.unmodifiableMap(typeVariableMap));
        }
        return typeVariableMap;
    }

    private static void buildTypeVariableMap(ResolvableType type, Map<? super TypeVariable<?>, ? super Type> typeVariableMap) {
        if (type != ResolvableType.NONE) {
            Class<?> resolved = type.resolve();
            if (resolved != null && type.getType() instanceof ParameterizedType) {
                var variables = resolved.getTypeParameters();
                for (int i = 0; i < variables.length; i++) {
                    ResolvableType generic = type.getGeneric(i);
                    while (generic.getType() instanceof TypeVariable<?>) {
                        generic = generic.resolveType();
                    }
                    if (!generic.equals(ResolvableType.NONE)) {
                        typeVariableMap.put(variables[i], generic.getType());
                    }
                }
            }
            buildTypeVariableMap(type.getSuperType(), typeVariableMap);
            for (ResolvableType interfaceType : type.getInterfaces()) {
                buildTypeVariableMap(interfaceType, typeVariableMap);
            }
            if (resolved != null && resolved.isMemberClass()) {
                buildTypeVariableMap(ResolvableType.forClass(resolved.getEnclosingClass()), typeVariableMap);
            }
        }
    }
}
