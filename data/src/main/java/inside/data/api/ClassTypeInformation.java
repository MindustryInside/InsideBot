package inside.data.api;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ClassTypeInformation<S> extends TypeDiscoverer<S> {

    private static final ConcurrentMap<Class<?>, ClassTypeInformation<?>> cache = new ConcurrentHashMap<>();

    private final Class<S> type;

    ClassTypeInformation(Class<S> type) {
        super(type, getTypeVariableMap(type));
        this.type = Objects.requireNonNull(type, "type");
    }

    @SuppressWarnings("unchecked")
    public static <S> ClassTypeInformation<S> from(Class<S> type) {
        return (ClassTypeInformation<S>) cache.computeIfAbsent(type, ClassTypeInformation::new);
    }

    private static Map<TypeVariable<?>, Type> getTypeVariableMap(Class<?> type) {
        return getTypeVariableMap(type, new HashSet<>());
    }

    private static Map<TypeVariable<?>, Type> getTypeVariableMap(Class<?> type, Set<? super Type> visited) {
        if (visited.contains(type)) {
            return Map.of();
        }

        visited.add(type);

        var source = GenericTypeResolver.getTypeVariableMap(type);
        Map<TypeVariable<?>, Type> map = new HashMap<>(source.size());

        for (var entry : source.entrySet()) {
            Type value = entry.getValue();
            map.put(entry.getKey(), value);

            if (value instanceof Class<?> c) {
                getTypeVariableMap(c, visited).forEach(map::putIfAbsent);
            }
        }

        return map;
    }

    @Override
    public Class<S> getType() {
        return type;
    }
}
