package inside.data.service;

import inside.data.entity.base.BaseEntity;
import reactor.core.publisher.Mono;

public interface LongObjEntityService<V extends BaseEntity> extends EntityService<Long, V>{

    @Override
    default Mono<V> find(Long id){
        return find((long)id);
    }

    Mono<V> find(long id);

    @Override
    default Mono<Void> delete(Long id){
        return delete((long)id);
    }

    Mono<Void> delete(long id);
}
