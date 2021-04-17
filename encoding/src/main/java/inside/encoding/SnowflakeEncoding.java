package inside.encoding;

import discord4j.common.util.Snowflake;
import org.immutables.encode.Encoding;

import java.util.Objects;

@Encoding
public class SnowflakeEncoding{

    @Encoding.Impl
    private long value;

    @Encoding.Expose
    long get(){
        return value;
    }

    @Override
    public String toString(){
        return Long.toUnsignedString(value);
    }

    @Override
    public int hashCode(){
        return Long.hashCode(value);
    }

    public boolean equals(SnowflakeEncoding obj){
        return value == obj.value;
    }

    @Encoding.Copy
    public long withLong(long value){
        return value;
    }

    @Encoding.Copy
    public long withString(String value){
        return Long.parseUnsignedLong(value);
    }

    @Encoding.Builder
    static class Builder{

        private long id;

        @Encoding.Init
        public void setStringValue(String value){
            this.id = Long.parseUnsignedLong(value);
        }

        @Encoding.Init
        public void setLongValue(long value){
            this.id = value;
        }

        @Encoding.Copy
        public void copySnowflake(Snowflake value){
            this.id = Objects.requireNonNull(value, "value").asLong();
        }

        @Encoding.Build
        long build(){
            return this.id;
        }
    }
}
