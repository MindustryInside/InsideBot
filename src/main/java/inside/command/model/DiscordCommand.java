package inside.command.model;

import discord4j.rest.util.Permission;
import inside.command.CommandCategory;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Component
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DiscordCommand{

    String[] key();

    String params() default "";

    String description();

    Permission[] permissions() default {Permission.SEND_MESSAGES, Permission.EMBED_LINKS};

    CommandCategory category() default CommandCategory.common;
}
