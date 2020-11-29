package insidebot.data.service;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import insidebot.data.entity.GuildConfig;

import java.util.function.Supplier;

public interface GuildService{
    GuildConfig get(Guild guild);

    GuildConfig get(Snowflake guildId);

    GuildConfig getOr(Snowflake guildId, Supplier<GuildConfig> prov);

    GuildConfig save(GuildConfig entity);

    boolean exists(Snowflake guildId);

    String prefix(Snowflake guildId);

    String locale(Snowflake guildId);

    Snowflake logChannelId(Snowflake guildId);

    Snowflake muteRoleId(Snowflake guildId);

    Snowflake activeUserRoleId(Snowflake guildId);

    boolean auditDisabled(Snowflake guildId);

    boolean muteDisabled(Snowflake guildId);

    boolean activeUserDisabled(Snowflake guildId);
}
