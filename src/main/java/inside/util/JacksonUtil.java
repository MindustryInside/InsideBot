package inside.util;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.util.*;

@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class JacksonUtil{
    public static final ObjectMapper mapper = new ObjectMapper();

    static{
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    public static ObjectMapper mapper(){
        return mapper;
    }

    public static <T> T fromString(String string, Class<T> clazz){
        try{
            return mapper.readValue(string, clazz);
        }catch(Throwable t){
            throw new RuntimeException(t);
        }
    }

    public static String toString(Object value){
        try{
            return mapper.writeValueAsString(value);
        }catch(Throwable t){
            throw new RuntimeException(t);
        }
    }

    public static JsonNode toJsonNode(Object value){
        return toJsonNode(toString(value));
    }

    public static JsonNode toJsonNode(String value){
        try{
            return mapper.readTree(value);
        }catch(Throwable t){
            throw new RuntimeException(t);
        }
    }

    public static <T> T clone(T value){
        return fromString(toString(value), (Class<T>)value.getClass());
    }

    public static List mapJsonToObjectList(String json, Class clazz){
        List list;
        TypeFactory type = TypeFactory.defaultInstance();
        try{
            list = mapper.readValue(json, type.constructCollectionType(ArrayList.class, clazz));
        }catch(Throwable t){
            throw new RuntimeException(t);
        }
        return list;
    }

    public static Map mapJsonToMap(String json, Class keyClass, Class valueClass){
        Map map;
        TypeFactory type = TypeFactory.defaultInstance();
        try{
            map = mapper.readValue(json, type.constructMapType(HashMap.class, keyClass, valueClass));
        }catch(Throwable t){
            throw new RuntimeException(t);
        }
        return map;
    }
}
