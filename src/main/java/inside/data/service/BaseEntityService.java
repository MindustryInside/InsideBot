package inside.data.service;

import inside.Settings;
import inside.data.entity.base.GuildEntity;
import inside.data.repository.base.GuildRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

public abstract class BaseEntityService<K, V extends GuildEntity, R extends GuildRepository<V>> implements EntityService<K, V>{

    protected final R repository;

    protected final Settings settings;

    protected final Object $lock = new Object[0];

    protected BaseEntityService(R repository, Settings settings){
        this.repository = repository;
        this.settings = settings;
    }

    @Override
    public V find(K id){
        Objects.requireNonNull(id, "id");

        V entity = get(id);
        if(entity == null){
            synchronized($lock){
                entity = get(id);
                if(entity == null){
                    entity = create(id);
                    save(entity);
                }
            }
        }
        return entity;
    }

    @Override
    @Transactional
    public void save(V entity){
        repository.save(entity);
    }

    @Override
    @Transactional
    public void delete(K id){
        Objects.requireNonNull(id, "id");
        repository.delete(get(id));
    }

    @Override
    @Transactional
    public void delete(V entity){
        repository.delete(entity);
    }

    protected abstract V create(K id);

    protected abstract V get(K id);

    protected void cleanUp(){
        // no-op
    }
}
