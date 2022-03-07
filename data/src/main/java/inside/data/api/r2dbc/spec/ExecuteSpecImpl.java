package inside.data.api.r2dbc.spec;

import inside.data.api.r2dbc.*;
import inside.util.Reflect;
import io.r2dbc.spi.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ExecuteSpecImpl implements ExecuteSpec {
    private final DatabaseClient databaseClient;
    private final Map<Integer, Parameter> indexParameters;
    private final Map<String, Parameter> nameParameters;
    private final Supplier<String> sqlSupplier;
    @Nullable
    private final TransactionDefinition definition;
    @Nullable
    private final R2dbcConnection connection;

    public ExecuteSpecImpl(DatabaseClient databaseClient, Supplier<String> sqlSupplier) {
        this(databaseClient, Map.of(), Map.of(), sqlSupplier, null, null);
    }

    public ExecuteSpecImpl(DatabaseClient databaseClient, Supplier<String> sqlSupplier, R2dbcConnection connection) {
        this(databaseClient, Map.of(), Map.of(), sqlSupplier, null, connection);
    }

    public ExecuteSpecImpl(DatabaseClient databaseClient,
                           Map<Integer, Parameter> indexParameters,
                           Map<String, Parameter> nameParameters,
                           Supplier<String> sqlSupplier,
                           @Nullable TransactionDefinition definition,
                           @Nullable R2dbcConnection connection) {
        this.databaseClient = databaseClient;
        this.indexParameters = indexParameters;
        this.nameParameters = nameParameters;
        this.sqlSupplier = sqlSupplier;
        this.definition = definition;
        this.connection = connection;
    }

    @Override
    public ExecuteSpec bind(int index, Object value) {
        var indexParameters = new LinkedHashMap<>(this.indexParameters);
        if (value instanceof Parameter p) {
            indexParameters.put(index, p);
        } else {
            indexParameters.put(index, Parameters.in(value));
        }
        return new ExecuteSpecImpl(databaseClient, indexParameters, nameParameters,
                sqlSupplier, definition, connection);
    }

    @Override
    public ExecuteSpec bindNull(int index, Class<?> type) {
        var indexParameters = new LinkedHashMap<>(this.indexParameters);
        indexParameters.put(index, Parameters.in(
                Reflect.wrapIfPrimitive(type)));

        return new ExecuteSpecImpl(databaseClient, indexParameters, nameParameters,
                sqlSupplier, definition, connection);
    }

    @Override
    public ExecuteSpec bind(String name, Object value) {
        var nameParameters = new LinkedHashMap<>(this.nameParameters);
        if (value instanceof Parameter p) {
            nameParameters.put(name, p);
        } else {
            nameParameters.put(name, Parameters.in(value));
        }
        return new ExecuteSpecImpl(databaseClient, indexParameters, nameParameters,
                sqlSupplier, definition, connection);
    }

    @Override
    public ExecuteSpec bindNull(String name, Class<?> type) {
        var nameParameters = new LinkedHashMap<>(this.nameParameters);
        nameParameters.put(name, Parameters.in(
                Reflect.wrapIfPrimitive(type)));

        return new ExecuteSpecImpl(databaseClient, indexParameters, nameParameters,
                sqlSupplier, definition, connection);
    }

    @Override
    public ExecuteSpec transactional(TransactionDefinition definition) {
        return new ExecuteSpecImpl(databaseClient, indexParameters, nameParameters,
                sqlSupplier, definition, connection);
    }

    @Override
    public <R> RowsFetchSpec<R> map(Function<? super R2dbcRow, ? extends R> mappingFunction) {
        return execute((row, rowMetadata) -> mappingFunction.apply(R2dbcRow.of(row)));
    }

    @Override
    public FetchSpec<R2dbcRow> fetch() {
        return execute((row, rowMetadata) -> R2dbcRow.of(row));
    }

    @Override
    public Mono<Void> then() {
        return fetch().rowsUpdated().then();
    }

    private <T> FetchSpec<T> execute(BiFunction<Row, RowMetadata, ? extends T> mappingFunction) {
        String sql = Objects.requireNonNull(sqlSupplier.get(), "sql");

        Function<R2dbcConnection, R2dbcStatement> statementFunction = connection -> {
            R2dbcStatement statement = connection.createStatement(sql);

            indexParameters.forEach(statement::bindOptional);
            nameParameters.forEach(statement::bindOptional);

            return statement;
        };

        Function<R2dbcConnection, Flux<R2dbcResult>> resultFunction = connection -> {
            R2dbcStatement statement = statementFunction.apply(connection);

            Flux<R2dbcResult> exp = statement.execute()
                    .checkpoint("SQL \"" + sql + "\" [DatabaseClient]");

            if (definition != null) {
                Mono<TransactionDefinition> resource = connection.beginTransaction()
                        .thenReturn(definition);

                return Flux.usingWhen(resource, def -> exp,
                        def -> connection.commitTransaction(),
                        (def, t) -> connection.rollbackTransaction().thenMany(Flux.error(t)),
                        def -> connection.rollbackTransaction());
            }

            return exp;
        };

        Function<R2dbcConnection, Mono<Integer>> updatedRowsFunction = connection -> resultFunction.apply(connection)
                .flatMap(Result::getRowsUpdated)
                .collect(Collectors.summingInt(Integer::intValue));

        Mono<R2dbcConnection> connectionMono = Mono.justOrEmpty(connection)
                .switchIfEmpty(databaseClient.create());

        return new FetchSpecImpl<>(connectionMono, resultFunction,
                updatedRowsFunction, mappingFunction);
    }
}
