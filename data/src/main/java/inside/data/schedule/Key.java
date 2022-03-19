package inside.data.schedule;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import inside.util.Preconditions;
import org.immutables.value.Value;
import reactor.util.annotation.Nullable;

@Value.Immutable
@InlineFieldStyle
public abstract class Key implements Comparable<Key>{
    public static final String DEFAULT_GROUP = "default";

    @JsonCreator
    public static ImmutableKey parse(String text){
        String[] parts = text.split("\\.", 2);
        Preconditions.requireArgument(parts.length > 0, "Not a dot-separated key format");
        return ImmutableKey.of(parts[0], parts.length > 1 ? parts[1] : DEFAULT_GROUP);
    }

    public static ImmutableKey of(@Nullable String group, String name){
        return ImmutableKey.of(group == null ? DEFAULT_GROUP : group, name);
    }

    @Value.Default
    public String group(){
        return DEFAULT_GROUP;
    }

    public abstract String name();

    @JsonValue
    @Override
    public String toString(){
        return group() + '.' + name();
    }

    @Override
    public int compareTo(Key o){
        String group2 = o.group();
        if(DEFAULT_GROUP.equals(group()) && !DEFAULT_GROUP.equals(group2)){
            return -1;
        }
        if(!DEFAULT_GROUP.equals(group()) && DEFAULT_GROUP.equals(group2)){
            return 1;
        }

        int d = group().compareTo(group2);
        if(d != 0){
            return d;
        }

        return name().compareTo(o.name());
    }
}
