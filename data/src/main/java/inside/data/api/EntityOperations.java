package inside.data.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import discord4j.common.JacksonResources;
import inside.data.annotation.Entity;
import inside.data.api.descriptor.JavaTypeDescriptor;
import inside.data.api.descriptor.JsonDescriptor;
import inside.data.api.descriptor.OptionalDescriptor;
import inside.data.api.descriptor.SubEntityDescriptor;
import inside.data.api.r2dbc.R2dbcConnection;
import inside.data.api.r2dbc.R2dbcStatement;
import inside.data.api.r2dbc.RowMapper;
import inside.util.Reflect;
import io.r2dbc.spi.Result;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.annotation.Nullable;
import reactor.util.function.Tuples;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class EntityOperations {
    private final JacksonResources jacksonResources;
    private final Map<Type, JavaTypeDescriptor<?>> descriptors = new ConcurrentHashMap<>();
    private final Map<Class<?>, RelationEntityInformation<?>> information = new ConcurrentHashMap<>();

    public EntityOperations(JacksonResources jacksonResources) {
        this.jacksonResources = Objects.requireNonNull(jacksonResources, "jacksonResources");
    }

    @SuppressWarnings("unchecked")
    public <T> RelationEntityInformation<T> getInformation(Class<?> type) {
        return (RelationEntityInformation<T>) information.computeIfAbsent(type, RelationEntityInformation::compile);
    }

    public <T> Flux<T> selectAll(R2dbcConnection connection, RelationEntityInformation<? extends T> info) {
        return Flux.defer(() -> {

            RowMapper<T> rowMapper = EntityRowMapper.create(info, this);

            return connection.createStatement(QueryUtil.createSelectAllSql(info)).execute()
                    .flatMap(result -> result.mapWith(rowMapper));
        });
    }

    public <T> Mono<T> select(R2dbcConnection connection, RelationEntityInformation<? extends T> info, Object id) {
        return Mono.defer(() -> {

            RowMapper<T> rowMapper = EntityRowMapper.create(info, this);

            return connection.createStatement(QueryUtil.createSelectSql(info, this))
                    .bindOptional(0, id, Reflect.wrapIfPrimitive(id.getClass()))
                    .execute()
                    .flatMap(result -> result.mapWith(rowMapper))
                    .singleOrEmpty();
        });
    }

    public <T> Mono<T> save(R2dbcConnection connection, RelationEntityInformation<T> info, T entity) {
        Mono<T> cascadeUpdate = Flux.fromIterable(info.getCandidateProperties())
                .filter(prop -> prop.getClassType().isAnnotationPresent(Entity.class))
                .mapNotNull(prop -> {
                    Object o = prop.getValue(entity);
                    if (o == null) {
                        return null;
                    }

                    return Tuples.of(prop, o);
                })
                .flatMap(TupleUtils.function((prop, obj) -> save(connection,
                        getInformation(prop.getClassType()), obj)
                        .filter(o -> !o.equals(obj)) // не будем переустанавливать неизмененные поля-объекты
                        .map(o -> Tuples.of(prop, o))))
                .collectList()
                .map(list -> {
                    // Обновляем entity новыми подобъектами
                    var factory = BuilderMethods.of(info.getType());
                    Object builder = Reflect.invoke(factory.getBuilder(), null);
                    Reflect.invoke(factory.getFromMethod(), builder, entity);

                    for (var tuple : list) {
                        factory.getMethods().stream()
                                .filter(m -> {
                                    Type[] params = m.getGenericParameterTypes();
                                    return m.getName().equals(tuple.getT1().getMethod().getName()) &&
                                            params.length == 1 && params[0].equals(tuple.getT1().getType());
                                })
                                .findFirst()
                                .ifPresent(m -> Reflect.invoke(m, builder, tuple.getT2()));
                    }

                    return Reflect.invoke(factory.getBuild(), builder);
                })
                .cast(info.getType());

        if (info.isNew(entity)) {
            return cascadeUpdate.flatMap(t -> insert(connection, info, t));
        }
        return cascadeUpdate.flatMap(t -> update(connection, info, t).thenReturn(t));
    }

    public <T> Mono<Integer> update(R2dbcConnection connection, RelationEntityInformation<T> info, T object) {
        return Mono.defer(() -> {

            R2dbcStatement statement = connection.createStatement(
                    QueryUtil.createUpdateSql(info));

            var properties = info.getCandidateProperties();
            for (int i = 0; i < properties.size(); i++) {
                PersistentProperty property = properties.get(i);
                var descriptor = getDescriptor(property);
                Class<?> fieldType = descriptor != null
                        ? descriptor.getSqlType()
                        : Reflect.wrapIfPrimitive(property.getClassType());
                Object obj = property.getValue(object);
                Object fieldObj = descriptor != null ? descriptor.unwrap(obj) : obj;

                statement.bindOptional(i, fieldObj, fieldType);
            }

            var idProperty = info.getIdProperty();
            var descriptor = getDescriptor(idProperty);
            Class<?> fieldType = descriptor != null
                    ? descriptor.getSqlType()
                    : Reflect.wrapIfPrimitive(idProperty.getClassType());
            Object obj = idProperty.getValue(object);
            Object fieldObj = descriptor != null ? descriptor.unwrap(obj) : obj;

            statement.bindOptional(properties.size(), fieldObj, fieldType);

            return statement.execute()
                    .flatMap(Result::getRowsUpdated)
                    .singleOrEmpty();
        });
    }

    public <T> Mono<T> insert(R2dbcConnection connection, RelationEntityInformation<? extends T> info, T object) {
        return Mono.defer(() -> {

            R2dbcStatement statement = connection.createStatement(
                    QueryUtil.createInsertSql(info));

            var candidateProperties = info.getCandidateProperties();
            for (int i = 0; i < candidateProperties.size(); i++) {
                PersistentProperty property = candidateProperties.get(i);
                var descriptor = getDescriptor(property);
                Class<?> fieldType = descriptor != null
                        ? descriptor.getSqlType()
                        : Reflect.wrapIfPrimitive(property.getClassType());
                Object obj = property.getValue(object);
                Object fieldObj = descriptor != null ? descriptor.unwrap(obj) : obj;

                statement.bindOptional(i, fieldObj, fieldType);
            }

            var generated = info.getGeneratedProperties().stream()
                    .map(PersistentProperty::getName)
                    .toArray(String[]::new);

            RowMapper<T> rowMapper = EntityIdUpdateRowMapper.create(object, info, this);

            return statement.returnGeneratedValues(generated).execute()
                    .flatMap(result -> result.mapWith(rowMapper))
                    .singleOrEmpty();
        });
    }

    public <T> Mono<Integer> delete(R2dbcConnection connection, RelationEntityInformation<T> info, Object object) {
        return Mono.defer(() -> {

            R2dbcStatement statement = connection.createStatement(
                    QueryUtil.createDeleteSql(info));

            var idProperty = info.getIdProperty();
            var descriptor = getDescriptor(idProperty);
            Class<?> fieldType = descriptor != null
                    ? descriptor.getSqlType()
                    : Reflect.wrapIfPrimitive(idProperty.getClassType());
            Object obj = idProperty.getValue(object);
            Object fieldObj = descriptor != null ? descriptor.unwrap(obj) : obj;

            statement.bindOptional(0, fieldObj, fieldType);

            return statement.execute()
                    .flatMap(Result::getRowsUpdated)
                    .singleOrEmpty();
        });
    }

    public <T> Mono<Long> count(R2dbcConnection connection, RelationEntityInformation<T> info) {
        return connection.createStatement(QueryUtil.createCountSql(info)).execute().next()
                .flatMap(result -> result.mapWith(row -> row.getLong(0))
                        .singleOrEmpty());
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> JavaTypeDescriptor<T> getDescriptor(PersistentProperty prop) {
        Type type = prop.getType();
        if (prop.getClassType().isAnnotationPresent(Entity.class)) {
            return (JavaTypeDescriptor<T>) descriptors.computeIfAbsent(type,
                    type1 -> new SubEntityDescriptor(getInformation(prop.getClassType()).getIdProperty()));
        }

        if (isJsonSerial(type)) {
            return (JavaTypeDescriptor<T>) descriptors.computeIfAbsent(type,
                    type1 -> new JsonDescriptor(jacksonResources, type1));
        }

        if (type instanceof ParameterizedType p && p.getRawType() instanceof Class<?> c && c == Optional.class) {
            Type t = p.getActualTypeArguments()[0];
            Class<?> c1 = t instanceof Class<?> c2 ? c2 : t instanceof ParameterizedType p1 ? (Class<?>) p1.getRawType() : null;

            return (JavaTypeDescriptor<T>) descriptors.computeIfAbsent(type, type1 -> new OptionalDescriptor<>(c1));
        }

        return null;
    }

    private boolean isJsonSerial(Type type) {
        return type instanceof Class<?> c && isJson(c) ||
                type instanceof ParameterizedType p && (isJson(p.getRawType()) ||
                        Arrays.stream(p.getActualTypeArguments())
                                .allMatch(this::isJson));
    }

    private boolean isJson(Type type) {
        ObjectMapper mapper = jacksonResources.getObjectMapper();
        Class<?> raw = Reflect.toClass(type);

        return raw.isAnnotationPresent(JsonDeserialize.class) &&
                raw.isAnnotationPresent(JsonSerialize.class) &&
                mapper.canSerialize(raw) && mapper.canDeserialize(mapper.constructType(type)) ||
                Collection.class.isAssignableFrom(raw) || Map.class.isAssignableFrom(raw);
    }

}
