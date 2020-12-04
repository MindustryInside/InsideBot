package insidebot.common.command.model.base;

import discord4j.rest.util.Permission;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Component
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DiscordCommand{

    String key();

    String params() default "";

    String description();

    Permission[] permissions() default {Permission.SEND_MESSAGES, Permission.EMBED_LINKS};
}
