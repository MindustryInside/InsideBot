package inside.openweather.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.*;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableWindData.class)
@JsonDeserialize(as = ImmutableWindData.class)
public interface WindData{

    static ImmutableWindData.Builder builder(){
        return ImmutableWindData.builder();
    }

    float speed();

    @JsonProperty("deg")
    int degrees();

    float gust();
}
