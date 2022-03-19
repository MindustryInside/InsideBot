package inside.data.schedule;

import org.immutables.value.Value;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@Value.Style(
        visibility = Value.Style.ImplementationVisibility.PACKAGE,
        allParameters = true,
        depluralize = true
)
@interface InlineFieldStyle {
}
