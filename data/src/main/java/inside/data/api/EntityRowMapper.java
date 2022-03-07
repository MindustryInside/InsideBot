package inside.data.api;

import inside.data.api.r2dbc.R2dbcRow;
import inside.data.api.r2dbc.RowMapper;
import inside.util.Reflect;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.util.Objects;

public final class EntityRowMapper<T> implements RowMapper<T> {
    private static final Logger log = Loggers.getLogger(EntityRowMapper.class);

    private final RelationEntityInformation<? extends T> information;
    private final EntityOperations entityOperations;

    private EntityRowMapper(RelationEntityInformation<? extends T> information, EntityOperations entityOperations) {
        this.information = Objects.requireNonNull(information, "information");
        this.entityOperations = Objects.requireNonNull(entityOperations, "entityOperations");
    }

    public static <T> RowMapper<T> create(RelationEntityInformation<? extends T> information,
                                          EntityOperations entityOperations) {
        return new EntityRowMapper<>(information, entityOperations);
    }

    @Override
    public T apply(R2dbcRow row) {
        // class-type style
        if (!information.getType().isInterface()) {
            T instance = Reflect.instance(information.getType());

            for (PersistentProperty p : information.getProperties()) {
                if (p instanceof FieldPersistentProperty prop) {
                    var descriptor = entityOperations.getDescriptor(prop);
                    Object obj = descriptor != null
                            ? descriptor.wrap(row.get(prop.getName(), descriptor.getSqlType()))
                            : row.get(prop.getName(), Reflect.wrapIfPrimitive(prop.getClassType()));
                    Reflect.set(prop.getField(), instance, obj);
                }
            }

            return instance;
        }

        BuilderMethods factory = BuilderMethods.of(information.getType());
        Object builder = Reflect.invoke(factory.getBuilder(), null);

        for (PersistentProperty prop : information.getProperties()) {
            if (prop instanceof MethodPersistentProperty mprop) {
                var descriptor = entityOperations.getDescriptor(mprop);

                Object obj = descriptor != null
                        ? descriptor.wrap(row.get(mprop.getName(), descriptor.getSqlType()))
                        : row.get(mprop.getName(), Reflect.wrapIfPrimitive(mprop.getClassType()));

                factory.getMethods().stream()
                        .filter(m -> {
                            Class<?>[] params = m.getParameterTypes();
                            return m.getName().equals(mprop.getMethod().getName()) &&
                                    params.length == 1 && params[0].isAssignableFrom(mprop.getClassType());
                        })
                        .findFirst()
                        .ifPresentOrElse(m -> Reflect.invoke(m, builder, obj),
                                () -> log.error("Failed to resolve build method {} for type: {}",
                                        mprop.getMethod().getName(), information.getType()));
            }
        }

        return Reflect.invoke(factory.getBuild(), builder);
    }
}
