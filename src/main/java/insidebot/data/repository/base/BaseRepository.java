package insidebot.data.repository.base;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import reactor.core.publisher.Flux;

@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID>{

    default Flux<T> getAll(){
        return Flux.fromIterable(findAll());
    }
}
