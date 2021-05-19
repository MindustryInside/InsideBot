package inside.data.service;

import inside.Settings;
import inside.data.entity.base.BaseEntity;
import inside.data.repository.base.BaseRepository;
import reactor.core.publisher.Mono;

public abstract class BaseLongObjEntityService<V extends BaseEntity, R extends BaseRepository<V>>
        extends BaseEntityService<Long, V, R>
        implements LongObjEntityService<V>{

    protected BaseLongObjEntityService(R repository, Settings settings){
        super(repository, settings);
    }

    @Override
    public Mono<V> find(long id){
        return super.find(id);
    }

    @Override
    public Mono<Void> delete(long id){
        return super.delete(id);
    }
}
