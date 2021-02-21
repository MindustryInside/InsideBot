package inside.event.audit;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Component
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ForwardAuditProvider{

    AuditEventType value();
}


