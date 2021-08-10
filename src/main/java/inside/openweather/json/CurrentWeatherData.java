package inside.openweather.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.*;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableCurrentWeatherData.class)
@JsonDeserialize(as = ImmutableCurrentWeatherData.class)
public interface CurrentWeatherData{

    static ImmutableCurrentWeatherData.Builder builder(){
        return ImmutableCurrentWeatherData.builder();
    }

    @JsonProperty("coord")
    CoordinateData coordinate();

    List<WeatherData> weather();

    String base();

    MainData main();

    int visibility();

    WindData wind();

    CloudsData clouds();

    @JsonProperty("dt")
    long dateTime();

    @JsonProperty("sys")
    SystemData system();

    int timezone();

    int id();

    String name();

    @JsonProperty("cod")
    int code();
}
