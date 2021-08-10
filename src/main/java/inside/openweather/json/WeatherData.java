package inside.openweather.json;

import com.fasterxml.jackson.databind.annotation.*;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableWeatherData.class)
@JsonDeserialize(as = ImmutableWeatherData.class)
public interface WeatherData{

    static ImmutableWeatherData.Builder builder(){
        return ImmutableWeatherData.builder();
    }

    int id();

    String main();

    String description();

    String icon();
}
