package inside.openweather.json;

import com.fasterxml.jackson.databind.annotation.*;
import discord4j.discordjson.possible.Possible;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableSystemData.class)
@JsonDeserialize(as = ImmutableSystemData.class)
public interface SystemData{

    static ImmutableSystemData.Builder builder(){
        return ImmutableSystemData.builder();
    }

    Possible<Integer> type();

    Possible<Integer> id();

    String country();

    long sunrise();

    long sunset();
}
