package inside.data.service;

import inside.data.cache.EntityCacheManager;
import inside.data.entity.base.BaseEntity;
import inside.data.repository.base.BaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.*;
import reactor.util.annotation.Nullable;

public abstract class BaseEntityService<K, V extends BaseEntity, R extends BaseRepository<V>> implements EntityService<K, V>{

    protected final R repository;

    protected final boolean cache;

    @Autowired
    private EntityCacheManager entityCacheManager;

    protected BaseEntityService(R repository){
        this(repository, false);
    }

    protected BaseEntityService(R repository, boolean cache){
        this.repository = repository;
        this.cache = cache;
    }

    @Override
    public Mono<V> find(K id){
        if(cache){
            return Mono.fromSupplier(() -> entityCacheManager.get(getEntityType(), id, this::find0));
        }
        return Mono.fromSupplier(() -> find0(id));
    }

    @Override
    @Transactional
    public Flux<V> getAll(){
        return Flux.defer(() -> Flux.fromIterable(repository.findAll()));
    }

    @Nullable
    protected abstract V find0(K id);

    @Override
    @Transactional
    public Mono<Void> save(V entity){
        return Mono.fromRunnable(() -> {
            repository.save(entity);
            if(cache){
                entityCacheManager.evict(getEntityType(), entity.id());
            }
        });
    }

    @Override
    @Transactional
    public Mono<Void> delete(K id){
        return find(id).flatMap(this::delete);
    }

    @Override
    @Transactional
    public Mono<Void> delete(V entity){
        return Mono.fromRunnable(() -> repository.delete(entity));
    }

    @SuppressWarnings("unchecked")
    private Class<V> getEntityType(){
        return (Class<V>)ClassTypeInformation.from(getClass())
                .getRequiredSuperTypeInformation(EntityService.class)
                .getTypeArguments()
                .get(1).getType();
    }

    protected void cleanUp(){
        // no-op
    }
}
