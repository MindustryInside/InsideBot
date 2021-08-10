package inside.openweather.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.*;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableCoordinateData.class)
@JsonDeserialize(as = ImmutableCoordinateData.class)
public interface CoordinateData{

    static ImmutableCoordinateData.Builder builder(){
        return ImmutableCoordinateData.builder();
    }

    static ImmutableCoordinateData of(float longitude, float latitude){
        return ImmutableCoordinateData.of(longitude, latitude);
    }

    @JsonProperty("lon")
    float longitude();

    @JsonProperty("lat")
    float latitude();
}
