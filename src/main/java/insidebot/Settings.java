package insidebot;

import discord4j.rest.util.Color;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class Settings{
    @Value("${spring.application.token}")
    public String token;

    @Value("#{new Boolean('${spring.application.debug')}")
    public boolean debug = false;

    @Value("#{new java.util.Locale('${spring.application.locale')}")
    public Locale locale = Locale.ROOT;

    public String prefix = "$";

    public Color normalColor = Color.of(0xC4F5B7);

    public Color errorColor = Color.of(0xff3838);
}
