package inside.common.services;

import discord4j.common.util.Snowflake;

import java.util.Locale;

public interface ContextService{

    void init(Snowflake guildId);

    Locale locale();

    void locale(Locale locale);

    Locale localeOrDefault(String locale);

    Locale localeOrDefault(Snowflake guildId);

    void reset();
}
