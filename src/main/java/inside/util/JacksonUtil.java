package inside.util;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import discord4j.common.JacksonResources;

import java.util.*;

@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class JacksonUtil{
    private static final ObjectMapper mapper = new ObjectMapper();

    static{
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.PUBLIC_ONLY);
        mapper.setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY);
        mapper.registerModule(new Jdk8Module());
    }

    public static ObjectMapper mapper(){
        return mapper;
    }

    public static <T> T fromJson(String string, Class<T> clazz){
        try{
            return mapper.readValue(string, clazz);
        }catch(Throwable t){
            throw new RuntimeException(t);
        }
    }

    public static String toJson(Object value){
        try{
            return mapper.writeValueAsString(value);
        }catch(Throwable t){
            throw new RuntimeException(t);
        }
    }

    public static JsonNode toJsonNode(Object value){
        return toJsonNode(toJson(value));
    }

    public static JsonNode toJsonNode(String value){
        try{
            return mapper.readTree(value);
        }catch(Throwable t){
            throw new RuntimeException(t);
        }
    }

    public static <T> T copy(T value){
        return fromJson(toJson(value), (Class<T>)value.getClass());
    }

    public static List list(String json, Class clazz, Class type){
        try{
            return mapper.readValue(json, TypeFactory.defaultInstance().constructCollectionType(clazz.getConstructors().length != 0 ? clazz : ArrayList.class, type));
        }catch(Throwable t){
            throw new RuntimeException(t);
        }
    }

    public static Set set(String json, Class clazz, Class type){
        try{
            return mapper.readValue(json, TypeFactory.defaultInstance().constructCollectionType(clazz.getConstructors().length != 0 ? clazz : HashSet.class, type));
        }catch(Throwable t){
            throw new RuntimeException(t);
        }
    }

    public static Map map(String json, Class clazz, Class key, Class value){
        try{
            return mapper.readValue(json, TypeFactory.defaultInstance().constructMapType(clazz.getConstructors().length != 0 ? clazz : HashMap.class, key, value));
        }catch(Throwable t){
            throw new RuntimeException(t);
        }
    }
}
