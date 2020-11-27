package insidebot.data.repository;

import insidebot.data.entity.*;
import insidebot.data.repository.base.UserRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalUserRepository extends UserRepository<LocalUser>{

}
