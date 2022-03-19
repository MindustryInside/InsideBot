package inside.data.api;

import inside.util.Preconditions;
import reactor.core.Exceptions;
import reactor.util.annotation.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class BuilderMethods {

    private static final ConcurrentMap<Class<?>, BuilderMethods> cache = new ConcurrentHashMap<>();

    private final Method fromMethod;
    private final Method builder;
    private final List<Method> methods;
    private final Method build;

    private BuilderMethods(Method fromMethod, Method builder, List<Method> methods, Method build) {
        this.fromMethod = Objects.requireNonNull(fromMethod, "fromMethod");
        this.builder = Objects.requireNonNull(builder, "builder");
        this.methods = Objects.requireNonNull(methods, "methods");
        this.build = Objects.requireNonNull(build, "build");
    }

    private static BuilderMethods compile(Class<?> type) {
        Preconditions.requireArgument(type.getSimpleName().startsWith("Immutable") &&
                Modifier.isFinal(type.getModifiers()), () -> "No a immutable class: " + type);

        Method fromMethod = null;
        List<Method> methods = new ArrayList<>();
        Method build = null;

        Method builder = Arrays.stream(type.getDeclaredMethods())
                .filter(method -> Modifier.isStatic(method.getModifiers())
                        && method.getName().equals("builder"))
                .findFirst()
                .orElseThrow();

        builder.trySetAccessible();

        for (Method builderMethods : builder.getReturnType().getDeclaredMethods()) {
            if (Modifier.isPublic(builderMethods.getModifiers())) {
                builderMethods.trySetAccessible();
                if (builderMethods.getName().equals("from")) {
                    fromMethod = builderMethods;
                }

                if (Modifier.isFinal(builderMethods.getModifiers())) {
                    methods.add(builderMethods);
                } else if (builderMethods.getName().equals("build")) {
                    build = builderMethods;
                }
            }
        }

        Objects.requireNonNull(fromMethod, "fromMethod");
        Objects.requireNonNull(build, "build");

        return new BuilderMethods(fromMethod, builder, methods, build);
    }

    private static Class<?> findImmutable(Class<?> type) {
        if (Modifier.isFinal(type.getModifiers()) && type.getSimpleName().startsWith("Immutable")) {
            return type;
        }

        try {
            return Class.forName(type.getPackageName() + ".Immutable" + type.getSimpleName());
        } catch (Throwable t) {
            throw Exceptions.propagate(t);
        }
    }

    public static BuilderMethods of(Class<?> type) {
        return cache.computeIfAbsent(findImmutable(type), BuilderMethods::compile);
    }

    public Method getFromMethod() {
        return fromMethod;
    }

    public Method getBuilder() {
        return builder;
    }

    public List<Method> getMethods() {
        return methods;
    }

    public Method getBuild() {
        return build;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BuilderMethods that = (BuilderMethods) o;
        return fromMethod.equals(that.fromMethod) && builder.equals(that.builder)
                && methods.equals(that.methods) && build.equals(that.build);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromMethod, builder, methods, build);
    }

    @Override
    public String toString() {
        return "ImmutableFactory{" +
                "fromMethod=" + fromMethod +
                ", builder=" + builder +
                ", methods=" + methods +
                ", build=" + build +
                '}';
    }
}
