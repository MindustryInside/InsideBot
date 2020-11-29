package insidebot.data.service;

import discord4j.common.util.Snowflake;
import insidebot.data.entity.LocalUser;

import java.util.function.Supplier;

public interface UserService{

    LocalUser get(LocalUser user);

    LocalUser getOr(Snowflake userId, Supplier<LocalUser> prov);

    boolean exists(Snowflake userId);

    LocalUser getById(Snowflake userId);

    LocalUser save(LocalUser user);

    void delete(LocalUser user);

    void deleteById(Snowflake userId);
}
