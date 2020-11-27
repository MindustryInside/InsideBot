package insidebot.common.command.model.base;

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
}
