package inside.data.repository.base;

import inside.data.entity.base.GuildEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface GuildRepository<T extends GuildEntity> extends JpaRepository<T, String>{

    T findByGuildId(String id);

    boolean existsByGuildId(String id);

    void deleteByGuildId(String id);
}
