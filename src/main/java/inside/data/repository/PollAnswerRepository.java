package inside.data.repository;

import inside.data.entity.*;
import inside.data.repository.base.GuildRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PollAnswerRepository extends GuildRepository<PollAnswer>{
}
