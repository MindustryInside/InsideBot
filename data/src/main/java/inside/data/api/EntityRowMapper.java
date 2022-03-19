package inside.data.api;

import inside.data.annotation.Entity;
import inside.data.api.r2dbc.R2dbcRow;
import inside.data.api.r2dbc.RowMapper;
import inside.util.Reflect;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.util.Objects;

public final class EntityRowMapper<T> implements RowMapper<T> {
    private static final Logger log = Loggers.getLogger(EntityRowMapper.class);

    private final RelationEntityInformation<? extends T> info;
    private final EntityOperations entityOperations;

    private EntityRowMapper(RelationEntityInformation<? extends T> info, EntityOperations entityOperations) {
        this.info = Objects.requireNonNull(info, "info");
        this.entityOperations = Objects.requireNonNull(entityOperations, "entityOperations");
    }

    public static <T> RowMapper<T> create(RelationEntityInformation<? extends T> information,
                                          EntityOperations entityOperations) {
        return new EntityRowMapper<>(information, entityOperations);
    }

    @Override
    public T apply(R2dbcRow row) {
        BuilderMethods factory = BuilderMethods.of(info.getType());
        Object builder = Reflect.invoke(factory.getBuilder(), null);

        for (PersistentProperty prop : info.getProperties()) {
            Object obj;
            if (prop.getClassType().isAnnotationPresent(Entity.class)) {
                var fmapper = create(entityOperations.getInformation(prop.getClassType()), entityOperations);

                obj = fmapper.apply(row);
            } else {
                var descriptor = entityOperations.getDescriptor(prop);

                obj = descriptor != null
                        ? descriptor.wrap(row.get(prop.getName(), descriptor.getSqlType()))
                        : row.get(prop.getName(), Reflect.wrapIfPrimitive(prop.getClassType()));
            }

            factory.getMethods().stream()
                    .filter(m -> {
                        Class<?>[] params = m.getParameterTypes();
                        return m.getName().equals(prop.getMethod().getName()) &&
                                params.length == 1 && params[0].isAssignableFrom(prop.getClassType());
                    })
                    .findFirst()
                    .ifPresentOrElse(m -> Reflect.invoke(m, builder, obj),
                            () -> log.error("Failed to resolve build method {} for type: {}",
                                    prop.getMethod().getName(), info.getType()));
        }

        return Reflect.invoke(factory.getBuild(), builder);
    }
}
