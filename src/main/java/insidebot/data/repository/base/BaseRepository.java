package insidebot.data.repository.base;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import reactor.core.publisher.*;

import java.util.List;

/**
 * Абстракция для перевода данных в Flux и Mono
 */
@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID>{

    default Flux<T> getAll(){
        return Flux.fromIterable(findAll());
    }

    default Mono<T> getById(ID id){
        return Mono.justOrEmpty(findById(id));
    }
}
