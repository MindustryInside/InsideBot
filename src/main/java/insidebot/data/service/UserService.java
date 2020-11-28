package insidebot.data.service;

import discord4j.common.util.Snowflake;
import insidebot.data.entity.LocalUser;
import org.springframework.lang.NonNull;

import java.util.function.Supplier;

public interface UserService{

    LocalUser get(@NonNull LocalUser user);

    LocalUser getOr(@NonNull Snowflake userId, Supplier<LocalUser> prov);

    boolean exists(@NonNull Snowflake userId);

    LocalUser getById(@NonNull Snowflake userId);

    LocalUser save(@NonNull LocalUser user);

    void delete(@NonNull LocalUser user);

    void deleteById(@NonNull Snowflake userId);
}
