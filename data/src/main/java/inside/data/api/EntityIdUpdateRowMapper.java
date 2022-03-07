package inside.data.api;

import inside.data.api.r2dbc.R2dbcRow;
import inside.data.api.r2dbc.RowMapper;
import inside.util.Reflect;

import java.lang.reflect.Type;
import java.util.Objects;

public class EntityIdUpdateRowMapper<T> implements RowMapper<T> {

    private final T object;
    private final RelationEntityInformation<? extends T> info;
    private final EntityOperations entityOperations;

    private EntityIdUpdateRowMapper(T object, RelationEntityInformation<? extends T> info, EntityOperations entityOperations) {
        this.object = Objects.requireNonNull(object, "object");
        this.info = Objects.requireNonNull(info, "info");
        this.entityOperations = Objects.requireNonNull(entityOperations, "entityOperations");
    }

    public static <T> RowMapper<T> create(T object, RelationEntityInformation<? extends T> info,
                                          EntityOperations entityOperations) {
        return new EntityIdUpdateRowMapper<>(object, info, entityOperations);
    }

    @Override
    public T apply(R2dbcRow row) {
        // class-style entities
        if (!info.getType().isInterface()) {
            for (PersistentProperty p : info.getGeneratedProperties()) {
                if (p instanceof FieldPersistentProperty prop) {
                    var descriptor = entityOperations.getDescriptor(prop);
                    Object obj = descriptor != null
                            ? descriptor.wrap(row.get(prop.getName(), descriptor.getSqlType()))
                            : row.get(prop.getName(), Reflect.wrapIfPrimitive(prop.getClassType()));

                    Reflect.set(prop.getField(), object, obj);
                }
            }

            return object;
        }

        var factory = BuilderMethods.of(info.getType());
        Object builder = Reflect.invoke(factory.getBuilder(), null);
        Reflect.invoke(factory.getFromMethod(), builder, object);

        for (PersistentProperty prop : info.getGeneratedProperties()) {
            if (prop instanceof MethodPersistentProperty mprop) {
                var descriptor = entityOperations.getDescriptor(mprop);
                Object obj = descriptor != null
                        ? descriptor.wrap(row.get(mprop.getName(), descriptor.getSqlType()))
                        : row.get(mprop.getName(), Reflect.wrapIfPrimitive(mprop.getClassType()));

                factory.getMethods().stream()
                        .filter(m -> {
                            Type[] params = m.getGenericParameterTypes();
                            return m.getName().equals(mprop.getMethod().getName()) &&
                                    params.length == 1 && params[0].equals(mprop.getType());
                        })
                        .findFirst()
                        .ifPresent(m -> Reflect.invoke(m, builder, obj));
            }
        }

        return Reflect.invoke(factory.getBuild(), builder);
    }
}
