package inside.data.service;

import discord4j.common.util.Snowflake;
import inside.data.entity.LocalUser;

import java.util.function.Supplier;

public interface UserService{

    LocalUser get(Snowflake userId);

    LocalUser getOr(Snowflake userId, Supplier<LocalUser> prov);

    boolean exists(Snowflake userId);

    LocalUser save(LocalUser user);

    void delete(LocalUser user);

    void deleteById(Snowflake userId);
}
