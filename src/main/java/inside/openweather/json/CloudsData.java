package inside.openweather.json;

import com.fasterxml.jackson.databind.annotation.*;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudsData.class)
@JsonDeserialize(as = ImmutableCloudsData.class)
public interface CloudsData{

    static ImmutableCloudsData.Builder builder(){
        return ImmutableCloudsData.builder();
    }

    static ImmutableCloudsData of(int all){
        return ImmutableCloudsData.of(all);
    }

    int all();
}
