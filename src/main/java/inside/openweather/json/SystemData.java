package inside.openweather.json;

import com.fasterxml.jackson.databind.annotation.*;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableSystemData.class)
@JsonDeserialize(as = ImmutableSystemData.class)
public interface SystemData{

    static ImmutableSystemData.Builder builder(){
        return ImmutableSystemData.builder();
    }

    int type();

    int id();

    String country();

    long sunrise();

    long sunset();
}
