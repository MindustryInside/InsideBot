package inside.data.service;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import inside.data.entity.*;
import org.joda.time.DateTimeZone;

import java.util.*;

public interface EntityRetriever{

    // guild config

    GuildConfig getGuildById(Snowflake guildId);

    void save(GuildConfig entity);

    String getPrefix(Snowflake guildId);

    Locale getLocale(Snowflake guildId);

    DateTimeZone getTimeZone(Snowflake guildId);

    // audit config

    AuditConfig getAuditConfigById(Snowflake guildId);

    Optional<Snowflake> getLogChannelId(Snowflake guildId);

    void save(AuditConfig auditConfig);

    // admin config

    AdminConfig getAdminConfigById(Snowflake guildId);

    Optional<Snowflake> getMuteRoleId(Snowflake guildId);

    List<Snowflake> getAdminRoleIds(Snowflake guildId);

    void save(AdminConfig config);

    // member

    LocalMember getMember(Member member);

    void save(LocalMember member);
}
