package insidebot.data.repository.base;

import discord4j.common.util.Snowflake;
import insidebot.data.entity.base.UserEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface UserRepository<T extends UserEntity> extends JpaRepository<T, String>{

    T findByUserId(String userId);

    default T findByUserId(Snowflake userId){
        return findByUserId(userId.asString());
    }
}
