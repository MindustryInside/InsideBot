package insidebot.data.repository.base;

import discord4j.common.util.Snowflake;
import insidebot.data.entity.base.UserEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

@NoRepositoryBean
public interface UserRepository<T extends UserEntity> extends JpaRepository<T, String>{

    @Query("select u from #{#entityName} u where u.userId = :#{#userId.asString()}")
    T findByUserId(@Param("userId") Snowflake userId);
}
