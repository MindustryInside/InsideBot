package inside.data.service;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.*;
import inside.data.entity.*;
import org.joda.time.DateTimeZone;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.function.Supplier;

public interface EntityRetriever{

    // guild

    GuildConfig getGuild(Guild guild);

    GuildConfig getGuildById(Snowflake guildId);

    GuildConfig getGuildById(Snowflake guildId, Supplier<GuildConfig> prov);

    GuildConfig save(GuildConfig entity);

    boolean existsGuildById(Snowflake guildId);

    Flux<Snowflake> adminRolesIds(Snowflake guildId);

    String prefix(Snowflake guildId);

    Locale locale(Snowflake guildId);

    DateTimeZone timeZone(Snowflake guildId);

    Optional<Snowflake> logChannelId(Snowflake guildId);

    Optional<Snowflake> muteRoleId(Snowflake guildId);

    Optional<Snowflake> activeUserRoleId(Snowflake guildId);

    default boolean auditDisabled(Snowflake guildId){
        return activeUserRoleId(guildId).isEmpty();
    }

    default boolean muteDisabled(Snowflake guildId){
        return muteRoleId(guildId).isEmpty();
    }

    default boolean activeUserDisabled(Snowflake guildId){
        return activeUserRoleId(guildId).isEmpty();
    }

    // member

    List<LocalMember> getAllMembers();

    LocalMember getMember(Member member);

    LocalMember getMember(Member member, Supplier<LocalMember> prov);

    LocalMember getMemberById(Snowflake guildId, Snowflake userId);

    LocalMember getMemberById(Snowflake guildId, Snowflake userId, Supplier<LocalMember> prov);

    LocalMember save(LocalMember member);

    boolean existsMemberById(Snowflake guildId, Snowflake userId);
}
