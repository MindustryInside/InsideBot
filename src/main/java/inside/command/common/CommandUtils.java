package inside.command.common;

import inside.util.Preconditions;
import io.netty.handler.codec.http.QueryStringEncoder;

import java.util.*;

public abstract class CommandUtils{

    private CommandUtils(){
    }

    public static String expandQuery(String uri, Map<String, Object> values){
        QueryStringEncoder encoder = new QueryStringEncoder(uri);
        values.forEach((key, value) -> encoder.addParam(key, String.valueOf(value)));
        return encoder.toString();
    }


    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> mapOf(Object... values){
        Objects.requireNonNull(values, "values");
        Preconditions.requireArgument((values.length & 1) == 0, "length is odd");
        Map<K, V> map = new HashMap<>();

        for(int i = 0; i < values.length / 2; ++i){
            map.put((K)values[i * 2], (V)values[i * 2 + 1]);
        }

        return Map.copyOf(map);
    }
}
