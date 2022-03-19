package inside.data.repository.support;

import inside.data.DatabaseResources;
import inside.data.api.RelationEntityInformation;
import inside.data.repository.base.ReactiveRepository;
import io.r2dbc.spi.IsolationLevel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

public class BaseRepository<K, T> implements ReactiveRepository<K, T> {

    protected final DatabaseResources databaseResources;
    protected final Class<T> type;
    protected final RelationEntityInformation<T> info;

    public BaseRepository(DatabaseResources databaseResources, Class<T> type) {
        this.databaseResources = Objects.requireNonNull(databaseResources, "databaseResources");
        this.type = Objects.requireNonNull(type, "type");

        this.info = databaseResources.getEntityOperations().getInformation(type);
    }

    @Override
    public Mono<T> find(K id) {
        return databaseResources.getDatabaseClient()
                .transactional(connection -> databaseResources.getEntityOperations().select(connection, info, id))
                .withDefinition(IsolationLevel.READ_COMMITTED);
    }

    @Override
    public Flux<? extends T> findAll() {
        return databaseResources.getDatabaseClient()
                .transactionalMany(connection -> databaseResources.getEntityOperations().selectAll(connection, info))
                .withDefinition(IsolationLevel.READ_COMMITTED);
    }

    @Override
    public Mono<? extends T> save(T entity) {
        return databaseResources.getDatabaseClient()
                .transactional(connection -> databaseResources.getEntityOperations()
                        .save(connection, info, entity))
                .withDefinition(IsolationLevel.READ_COMMITTED);
    }

    @Override
    public Flux<? extends T> saveAll(Iterable<? extends T> entities) {
        return databaseResources.getDatabaseClient()
                .transactionalMany(connection -> Flux.fromIterable(entities)
                        .flatMap(entity -> databaseResources.getEntityOperations()
                                .save(connection, info, entity)))
                .withDefinition(IsolationLevel.READ_COMMITTED);
    }

    @Override
    public Mono<Long> count() {
        return databaseResources.getDatabaseClient()
                .transactional(connection -> databaseResources.getEntityOperations()
                        .count(connection, info))
                .withDefinition(IsolationLevel.READ_COMMITTED);
    }

    @Override
    public Mono<Integer> delete(T entity) {
        return databaseResources.getDatabaseClient()
                .transactional(connection -> databaseResources.getEntityOperations()
                        .delete(connection, info, entity))
                .withDefinition(IsolationLevel.READ_COMMITTED);
    }
}
