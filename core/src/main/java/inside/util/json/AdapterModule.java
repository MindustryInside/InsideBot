package inside.util.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.module.SimpleModule;
import discord4j.common.util.Snowflake;
import discord4j.rest.util.Color;
import io.r2dbc.postgresql.codec.Interval;

import java.io.Serial;

public class AdapterModule extends SimpleModule {
    @Serial
    private static final long serialVersionUID = 7001003760195089827L;

    public AdapterModule() {
        setMixInAnnotation(Snowflake.class, SnowflakeMixin.class);
        setMixInAnnotation(Color.class, ColorMixin.class);
        setMixInAnnotation(Interval.class, IntervalMixin.class);
    }

    static abstract class SnowflakeMixin {

        @JsonCreator
        static Snowflake of(long id) {
            return Snowflake.of(0L);
        }

        @JsonCreator
        static Snowflake of(String id) {
            return Snowflake.of(0L);
        }

        @JsonValue
        abstract String asString();
    }

    static abstract class ColorMixin {

        ColorMixin(@JsonProperty int rgb) {

        }

        @JsonValue
        abstract int getRGB();
    }

    static abstract class IntervalMixin {

        @JsonCreator
        static Interval parse(@JsonProperty String value) {
            return Interval.ZERO;
        }

        @JsonValue
        abstract String getValue();
    }
}
