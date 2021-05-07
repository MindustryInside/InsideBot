package inside.command.model;

import discord4j.common.util.Snowflake;
import inside.util.*;
import org.joda.time.DateTime;
import reactor.util.annotation.Nullable;

public class OptionValue{
    private final String value;

    public OptionValue(String value){
        this.value = value;
    }

    public String asString(){
        return value;
    }

    public long asLong(){
        return Strings.parseLong(value);
    }

    public boolean asBoolean(){
        return Boolean.parseBoolean(value);
    }

    // TODO replace
    @Nullable
    public Snowflake asSnowflake(){
        return MessageUtil.parseUserId(value);
    }

    @Nullable
    public DateTime asDateTime(){
        return MessageUtil.parseTime(value);
    }

    @Override
    public String toString(){
        return "OptionValue{" +
                "value='" + value + '\'' +
                '}';
    }
}
