package insidebot.data.service;

import discord4j.common.util.Snowflake;
import insidebot.data.entity.LocalUser;
import org.springframework.lang.NonNull;

import java.util.function.Supplier;

public interface UserService{

    LocalUser get(@NonNull LocalUser user);

    LocalUser getOr(@NonNull String userId, Supplier<LocalUser> prov);

    default LocalUser getOr(@NonNull Snowflake userId, Supplier<LocalUser> prov){
        return getOr(userId.asString(), prov);
    }

    boolean exists(@NonNull String userId);

    default boolean exists(@NonNull Snowflake userId){
        return exists(userId.asString());
    }

    LocalUser getById(@NonNull String userId);

    default LocalUser getById(@NonNull Snowflake userId){
        return getById(userId.asString());
    }

    LocalUser save(@NonNull LocalUser user);

    void delete(@NonNull LocalUser user);

    void deleteById(@NonNull String userId);

    default void deleteById(@NonNull Snowflake userId){
        deleteById(userId.asString());
    }
}
