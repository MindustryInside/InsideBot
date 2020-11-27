package insidebot.data.service;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.*;
import insidebot.data.entity.LocalMember;

import java.util.function.Supplier;

public interface MemberService{

    LocalMember get(Member member);

    LocalMember get(Guild guild, User user);

    LocalMember get(Snowflake guildId, Snowflake userId);

    LocalMember getOr(Member member, Supplier<LocalMember> prov);

    LocalMember getOr(Guild guild, User user, Supplier<LocalMember> prov);

    LocalMember getOr(Snowflake guildId, Snowflake userId, Supplier<LocalMember> prov);

    LocalMember save(LocalMember member);

    boolean exists(Snowflake guildId, Snowflake userId);

    boolean isAdmin(Member member);
}
