package inside.common.services;

import discord4j.common.util.Snowflake;
import org.joda.time.DateTimeZone;

import java.util.Locale;

public interface ContextService{

    void init(Snowflake guildId);

    Locale locale();

    void locale(Locale locale);

    DateTimeZone zone();

    Locale localeOrDefault(Snowflake guildId);

    void reset();
}
