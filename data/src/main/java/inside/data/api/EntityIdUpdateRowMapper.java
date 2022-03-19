package inside.data.api;

import inside.data.annotation.Entity;
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
        BuilderMethods factory = BuilderMethods.of(info.getType());
        Object builder = Reflect.invoke(factory.getBuilder(), null);
        Reflect.invoke(factory.getFromMethod(), builder, object);

        for (PersistentProperty prop : info.getGeneratedProperties()) {
            boolean isEntity = prop.getClassType().isAnnotationPresent(Entity.class);
            Object old = prop.getValue(object);
            Object obj = old;
            if (old != null && isEntity) {
                var fmapper = create(old, entityOperations.getInformation(prop.getClassType()), entityOperations);

                obj = fmapper.apply(row);
            } else if (!isEntity) {
                var descriptor = entityOperations.getDescriptor(prop);
                obj = descriptor != null
                        ? descriptor.wrap(row.get(prop.getName(), descriptor.getSqlType()))
                        : row.get(prop.getName(), Reflect.wrapIfPrimitive(prop.getClassType()));
            }

            Object obj0 = obj;

            factory.getMethods().stream()
                    .filter(m -> {
                        Type[] params = m.getGenericParameterTypes();
                        return m.getName().equals(prop.getMethod().getName()) &&
                                params.length == 1 && params[0].equals(prop.getType());
                    })
                    .findFirst()
                    .ifPresent(m -> Reflect.invoke(m, builder, obj0));
        }

        return Reflect.invoke(factory.getBuild(), builder);
    }
}
