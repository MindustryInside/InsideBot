package insidebot.data.services;

import arc.func.Prov;
import discord4j.common.util.Snowflake;
import insidebot.data.entity.UserInfo;
import org.springframework.lang.NonNull;

public interface UserService{

    UserInfo get(@NonNull UserInfo user);

    UserInfo getOr(@NonNull String userId, Prov<UserInfo> prov); // Может юзать жава апи???

    default UserInfo getOr(@NonNull Snowflake userId, Prov<UserInfo> prov){
        return getOr(userId.asString(), prov);
    }

    boolean exists(@NonNull String userId);

    default boolean exists(@NonNull Snowflake userId){
        return exists(userId.asString());
    }

    UserInfo getById(@NonNull String userId);

    default UserInfo getById(@NonNull Snowflake userId){
        return getById(userId.asString());
    }

    UserInfo save(@NonNull UserInfo user);

    void delete(@NonNull UserInfo user);

    void deleteById(@NonNull String userId);

    default void deleteById(@NonNull Snowflake userId){
        deleteById(userId.asString());
    }

    void activeUsers();

    void unmuteUsers();
}
