package inside.interaction.annotation;

import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.channel.Channel;

import java.lang.annotation.*;

@Repeatable(Options.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Option {

    String name();

    ApplicationCommandOption.Type type();

    boolean required() default false;

    Choice[] choices() default {};

    boolean autocomplete() default false;

    Channel.Type[] channelTypes() default {};

    double minValue() default -1;

    double maxValue() default -1;

    int minLength() default -1;

    int maxLength() default -1;
}
