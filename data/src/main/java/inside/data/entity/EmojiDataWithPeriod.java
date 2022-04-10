package inside.data.entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import discord4j.discordjson.json.EmojiData;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableEmojiDataWithPeriod.class)
@JsonDeserialize(as = ImmutableEmojiDataWithPeriod.class)
public interface EmojiDataWithPeriod {

    int DEFAULT_PERIOD = 5;

    static ImmutableEmojiDataWithPeriod.Builder builder() {
        return ImmutableEmojiDataWithPeriod.builder();
    }

    @Value.Default
    @Value.Auxiliary
    default int period() {
        return DEFAULT_PERIOD;
    }

    EmojiData emoji();
}
