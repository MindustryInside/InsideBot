package insidebot.data.repository.base;

import discord4j.common.util.Snowflake;
import insidebot.data.entity.base.UserEntity;
import org.springframework.data.repository.NoRepositoryBean;
import reactor.util.annotation.NonNull;

@NoRepositoryBean
public interface UserRepository<T extends UserEntity> extends BaseRepository<T, String>{
    T findByUserId(@NonNull String userId);

    default T findByUserId(@NonNull Snowflake userId){
        return findByUserId(userId.asString());
    }
}
