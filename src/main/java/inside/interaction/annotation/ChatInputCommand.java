package inside.interaction.annotation;

import inside.interaction.PermissionCategory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ChatInputCommand {

    String name();

    String description();

    PermissionCategory[] permissions() default {PermissionCategory.EVERYONE};
}
