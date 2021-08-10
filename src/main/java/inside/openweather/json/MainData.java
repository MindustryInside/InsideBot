package inside.openweather.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.*;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableMainData.class)
@JsonDeserialize(as = ImmutableMainData.class)
public interface MainData{

    static ImmutableMainData.Builder builder(){
        return ImmutableMainData.builder();
    }

    @JsonProperty("temp")
    float temperature();

    @JsonProperty("feels_like")
    float feelsLike();

    @JsonProperty("temp_min")
    float temperatureMin();

    @JsonProperty("temp_max")
    float temperatureMax();

    int pressure();

    int humidity();
}
