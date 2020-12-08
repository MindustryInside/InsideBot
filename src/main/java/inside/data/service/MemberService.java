package inside.data.service;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.*;
import inside.data.entity.LocalMember;

import java.util.function.Supplier;

public interface MemberService{

    LocalMember get(Member member);

    LocalMember get(Snowflake guildId, Snowflake userId);

    LocalMember getOr(Member member, Supplier<LocalMember> prov);

    LocalMember getOr(Snowflake guildId, Snowflake userId, Supplier<LocalMember> prov);

    LocalMember save(LocalMember member);

    boolean exists(Snowflake guildId, Snowflake userId);

    void delete(LocalMember member);

    void deleteById(Snowflake guildId, Snowflake userId);

    String detailName(Member member);
}
