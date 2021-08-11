package inside.data.repository;

import inside.data.entity.WelcomeMessage;
import inside.data.repository.base.GuildRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WelcomeMessageRepository extends GuildRepository<WelcomeMessage>{

}
