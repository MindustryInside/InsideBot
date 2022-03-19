package inside.data.schedule;

import discord4j.discordjson.MetaEncodingEnabled;
import org.immutables.value.Value;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Value.Style(
        depluralize = true,
        jdkOnly = true,
        allParameters = true,
        defaultAsDefault = true,
        deepImmutablesDetection = true
)
@MetaEncodingEnabled
@Target({ElementType.TYPE, ElementType.PACKAGE})
@Retention(RetentionPolicy.CLASS)
@interface DefaultStyle {

}
