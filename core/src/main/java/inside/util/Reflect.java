package inside.util;

import java.lang.reflect.*;

@SuppressWarnings("unchecked")
public abstract class Reflect {

    private Reflect() {
    }

    public static <T> T instance(Class<T> type, Object... args) {
        try {
            Constructor<T> constructor = (Constructor<T>) type.getDeclaredConstructors()[0];
            constructor.trySetAccessible();
            return constructor.newInstance(args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void set(Class<?> type, Object object, String name, Object value) {
        try {
            Field field = type.getDeclaredField(name);
            field.trySetAccessible();
            field.set(object, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T get(Class<?> type, Object object, String name) {
        try {
            Field field = type.getDeclaredField(name);
            field.trySetAccessible();
            return (T) field.get(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void set(Field field, Object object, Object value) {
        try {
            field.trySetAccessible();
            field.set(object, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T get(Field field, Object object) {
        try {
            field.trySetAccessible();
            return (T) field.get(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T invoke(Method method, Object object, Object... args) {
        try {
            method.trySetAccessible();
            return (T) method.invoke(object, args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke " + method, e);
        }
    }

    public static Class<?> unwrapIfWrapper(Class<?> type) {
        if (type == Byte.class) {
            return byte.class;
        } else if (type == Short.class) {
            return short.class;
        } else if (type == Integer.class) {
            return int.class;
        } else if (type == Long.class) {
            return long.class;
        } else if (type == Character.class) {
            return char.class;
        } else if (type == Float.class) {
            return float.class;
        } else if (type == Double.class) {
            return double.class;
        } else if (type == Boolean.class) {
            return boolean.class;
        } else if (type == Void.class) {
            return void.class;
        } else {
            return type;
        }
    }

    public static Class<?> wrapIfPrimitive(Class<?> type) {
        if (type == byte.class) {
            return Byte.class;
        } else if (type == short.class) {
            return Short.class;
        } else if (type == int.class) {
            return Integer.class;
        } else if (type == long.class) {
            return Long.class;
        } else if (type == char.class) {
            return Character.class;
        } else if (type == float.class) {
            return Float.class;
        } else if (type == double.class) {
            return Double.class;
        } else if (type == boolean.class) {
            return Boolean.class;
        } else if (type == void.class) {
            return Void.class;
        } else {
            return type;
        }
    }

    public static Class<?> toClass(Type type) {
        if (type instanceof Class c) {
            return c;
        } else if (type instanceof GenericArrayType gat) {
            return toClass(gat.getGenericComponentType()).arrayType();
        } else if (type instanceof ParameterizedType pt) {
            return toClass(pt.getRawType());
        } else if (type instanceof TypeVariable tv) {
            Type[] bounds = tv.getBounds();
            return bounds.length == 0 ? Object.class : toClass(bounds[0]);
        } else if (type instanceof WildcardType wt) {
            Type[] bounds = wt.getUpperBounds();
            return bounds.length == 0 ? Object.class : toClass(bounds[0]);
        } else {
            throw new UnsupportedOperationException("Cannot handle type class: " + type.getClass());
        }
    }
}
