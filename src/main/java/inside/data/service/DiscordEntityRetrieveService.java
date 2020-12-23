package inside.data.service;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.*;
import inside.data.entity.*;
import org.joda.time.DateTimeZone;
import reactor.core.publisher.Flux;

import java.util.Locale;
import java.util.function.Supplier;

public interface DiscordEntityRetrieveService{

    // guild

    GuildConfig getGuild(Guild guild);

    GuildConfig getGuildById(Snowflake guildId);

    GuildConfig getGuildById(Snowflake guildId, Supplier<GuildConfig> prov);

    GuildConfig saveGuild(GuildConfig entity);

    boolean existsGuildById(Snowflake guildId);

    Flux<Snowflake> adminRolesIds(Snowflake guildId);

    String prefix(Snowflake guildId);

    Locale locale(Snowflake guildId);

    DateTimeZone timeZone(Snowflake guildId);

    Snowflake logChannelId(Snowflake guildId);

    Snowflake muteRoleId(Snowflake guildId);

    Snowflake activeUserRoleId(Snowflake guildId);

    boolean auditDisabled(Snowflake guildId);

    boolean muteDisabled(Snowflake guildId);

    boolean activeUserDisabled(Snowflake guildId);

    // member

    LocalMember getMember(Member member);

    LocalMember getMember(Member member, Supplier<LocalMember> prov);

    LocalMember getMemberById(Snowflake guildId, Snowflake userId);

    LocalMember getMemberById(Snowflake guildId, Snowflake userId, Supplier<LocalMember> prov);

    LocalMember saveMember(LocalMember member);

    boolean existsMemberById(Snowflake guildId, Snowflake userId);

    void deleteMember(LocalMember member);

    void deleteMemberById(Snowflake guildId, Snowflake userId);
}
