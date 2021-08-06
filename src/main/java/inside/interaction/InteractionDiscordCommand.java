package inside.interaction;

import discord4j.rest.util.ApplicationCommandOptionType;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Component
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface InteractionDiscordCommand{

    String name();

    String description();

    ApplicationCommandOptionType type() default ApplicationCommandOptionType.UNKNOWN;
}
