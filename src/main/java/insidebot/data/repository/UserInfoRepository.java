package insidebot.data.repository;

import insidebot.data.entity.UserInfo;
import insidebot.data.repository.base.UserRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserInfoRepository extends UserRepository<UserInfo>{

}
