package inside.interaction;

import discord4j.core.object.command.ApplicationCommandOption;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Component
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface InteractionDiscordCommand{

    String name();

    String description();

    ApplicationCommandOption.Type type() default ApplicationCommandOption.Type.UNKNOWN;
}
