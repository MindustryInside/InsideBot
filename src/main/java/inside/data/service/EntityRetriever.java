package inside.data.service;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import inside.data.entity.*;
import org.joda.time.DateTimeZone;
import reactor.core.publisher.Flux;

import java.util.*;

public interface EntityRetriever{

    // guild

    GuildConfig getGuildById(Snowflake guildId);

    void save(GuildConfig entity);

    Flux<Snowflake> adminRolesIds(Snowflake guildId);

    String prefix(Snowflake guildId);

    Locale locale(Snowflake guildId);

    DateTimeZone timeZone(Snowflake guildId);

    Optional<Snowflake> logChannelId(Snowflake guildId);

    Optional<Snowflake> muteRoleId(Snowflake guildId);

    // member

    LocalMember getMember(Member member);

    void save(LocalMember member);
}
