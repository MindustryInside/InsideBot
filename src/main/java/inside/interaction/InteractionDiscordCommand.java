package inside.interaction;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Component
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface InteractionDiscordCommand{

}
