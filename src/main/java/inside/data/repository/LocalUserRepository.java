package inside.data.repository;

import inside.data.entity.*;
import inside.data.repository.base.UserRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalUserRepository extends UserRepository<LocalUser>{

}
