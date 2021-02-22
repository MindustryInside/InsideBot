package inside.common;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Component
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Collector{

}
