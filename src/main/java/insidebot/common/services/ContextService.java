package insidebot.common.services;

import discord4j.common.util.Snowflake;
import discord4j.rest.util.Color;

import java.util.Locale;

public interface ContextService{

    void init(Snowflake guildId);

    Locale locale();

    void locale(Locale locale);

    Color color();

    void color(Color color);

    Locale locale(String locale);

    Locale locale(Snowflake guildId);

    void reset();
}
