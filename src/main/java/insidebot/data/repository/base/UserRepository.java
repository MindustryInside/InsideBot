package insidebot.data.repository.base;

import discord4j.common.util.Snowflake;
import insidebot.data.entity.base.UserEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface UserRepository<T extends UserEntity> extends JpaRepository<T, String>{

    @Query(value = "select u from #{#entityName} u where u.userId = :#{#userId?.asString()}", nativeQuery = true)
    T findByUserId(Snowflake userId);
}
