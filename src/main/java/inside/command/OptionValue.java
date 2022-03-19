package inside.command;

import discord4j.common.util.Snowflake;
import inside.util.MessageUtil;
import inside.util.Strings;
import io.r2dbc.postgresql.codec.Interval;
import reactor.util.annotation.Nullable;

import java.util.Objects;

public record OptionValue(String value) {

    public OptionValue(String value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    public long asLong() {
        return Strings.parseLong(value);
    }

    public boolean asBoolean() {
        return Boolean.parseBoolean(value);
    }

    @Nullable
    public Snowflake asSnowflake() {
        return MessageUtil.parseId(value.replaceAll("[<>@!#&]", "")); // parse all possible id types
    }

    @Nullable
    public Interval asInterval() {
        return MessageUtil.parseInterval(value);
    }
}
