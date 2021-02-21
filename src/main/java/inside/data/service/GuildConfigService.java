package inside.data.service;

import discord4j.common.util.Snowflake;
import inside.data.entity.GuildConfig;

public interface GuildConfigService{

    GuildConfig getGuildById(Snowflake guildId);

    void save(GuildConfig entity);
}
