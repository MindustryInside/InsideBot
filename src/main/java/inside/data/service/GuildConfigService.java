package inside.data.service;

import discord4j.common.util.Snowflake;
import inside.data.entity.GuildConfig;
import org.joda.time.DateTimeZone;

import java.util.*;

public interface GuildConfigService{

    GuildConfig getGuildById(Snowflake guildId);

    void save(GuildConfig entity);

    default List<String> getAdminRolesIds(Snowflake guildId){
        return getGuildById(guildId).adminRoleIDs();
    }

    default String getPrefix(Snowflake guildId){
        return getGuildById(guildId).prefix();
    }

    default Locale getLocale(Snowflake guildId){
        return getGuildById(guildId).locale();
    }

    default DateTimeZone getTimeZone(Snowflake guildId){
        return DateTimeZone.forTimeZone(getGuildById(guildId).timeZone());
    }

    default Optional<Snowflake> getLogChannelId(Snowflake guildId){
        return getGuildById(guildId).logChannelId();
    }

    default Optional<Snowflake> getMuteRoleId(Snowflake guildId){
        return getGuildById(guildId).muteRoleID();
    }
}
