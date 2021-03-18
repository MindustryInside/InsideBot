package inside.data.service;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import inside.data.entity.*;
import org.joda.time.DateTimeZone;
import reactor.core.publisher.Flux;

import java.util.*;

public interface EntityRetriever{

    // guild config

    GuildConfig getGuildById(Snowflake guildId);

    void save(GuildConfig entity);

    String getPrefix(Snowflake guildId);

    Locale getLocale(Snowflake guildId);

    DateTimeZone getTimeZone(Snowflake guildId);

    Optional<Snowflake> getLogChannelId(Snowflake guildId);

    // admin config

    AdminConfig getAdminConfigById(Snowflake guildId);

    Optional<Snowflake> getMuteRoleId(Snowflake guildId);

    List<Snowflake> adminRolesIds(Snowflake guildId);

    void save(AdminConfig config);

    // member

    LocalMember getMember(Member member);

    void save(LocalMember member);
}
