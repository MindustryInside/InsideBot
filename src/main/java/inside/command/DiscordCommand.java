package inside.command;

import discord4j.rest.util.Permission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DiscordCommand {

    String[] key();

    String params() default "";

    String description();

    Permission[] permissions() default {Permission.SEND_MESSAGES, Permission.EMBED_LINKS};

    CommandCategory category() default CommandCategory.common;
}
