package inside.data.type.descriptor;

import inside.util.*;
import org.hibernate.annotations.common.reflection.java.JavaXMember;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.*;
import org.hibernate.usertype.DynamicParameterizedType;
import reactor.util.annotation.Nullable;
import reactor.util.function.*;

import java.io.Serial;
import java.lang.reflect.*;
import java.util.*;

import static reactor.function.TupleUtils.*;

@SuppressWarnings({"unchecked", "deprecation", "rawtypes"})
public class JsonTypeDescriptor extends AbstractTypeDescriptor<Object> implements DynamicParameterizedType{
    @Serial
    private static final long serialVersionUID = -4842954367890483417L;

    public static final JsonTypeDescriptor instance = new JsonTypeDescriptor();

    private final List<Tuple2<Class<?>, Type>> types = new ArrayList<>();

    public JsonTypeDescriptor(){
        super(Object.class, new MutableMutabilityPlan<>(){
            @Serial
            private static final long serialVersionUID = 1606718143878984537L;

            @Override
            protected Object deepCopyNotNull(Object value){
                return JacksonUtil.copy(value);
            }
        });
    }

    @Override
    public void setParameterValues(Properties parameters){
        try{
            Class<?> clazz = ((ParameterType)parameters.get(PARAMETER_TYPE)).getReturnedClass();
            Field typeField = JavaXMember.class.getDeclaredField("type");
            if(!typeField.isAccessible()){
                typeField.setAccessible(true);
            }
            Type type = (Type)typeField.get(parameters.get(XPROPERTY));
            types.add(Tuples.of(clazz, type));
        }catch(IllegalAccessException | NoSuchFieldException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean areEqual(Object a, Object b){
        if(a == b) return true;
        if(a == null || b == null) return false;
        return JacksonUtil.toJsonNode(a).equals(JacksonUtil.toJsonNode(b));
    }

    @Override
    public String toString(Object value){
        return JacksonUtil.toJson(value);
    }

    @Override
    public Object fromString(String string){
        return types.stream()
                .filter(predicate((clazz, type) -> Try.ofCallable(() -> fromString0(string, clazz, type)).isSuccess()))
                .findFirst()
                .map(function((clazz, type) -> fromString0(string, clazz, type)))
                .orElse(null);
    }

    private Object fromString0(String string, Class<?> clazz, Type type){
        if(type instanceof ParameterizedType pType){
            if(List.class.isAssignableFrom((Class)pType.getRawType())){
                return JacksonUtil.list(string, clazz, (Class)pType.getActualTypeArguments()[0]);
            }else if(Map.class.isAssignableFrom((Class)pType.getRawType())){
                return JacksonUtil.map(string, clazz, (Class)pType.getActualTypeArguments()[0],
                        (Class)pType.getActualTypeArguments()[1]);
            }else if(Set.class.isAssignableFrom((Class)pType.getRawType())){
                return JacksonUtil.set(string, clazz, (Class)pType.getActualTypeArguments()[0]);
            }
        }
        return JacksonUtil.fromJson(string, clazz);
    }

    @Override
    public <X> X unwrap(@Nullable Object value, Class<X> type, WrapperOptions options){
        if(value == null){
            return null;
        }

        if(String.class.isAssignableFrom(type)){
            return (X)toString(value);
        }

        if(Object.class.isAssignableFrom(type)){
            return (X)JacksonUtil.toJsonNode(value);
        }

        throw unknownUnwrap(type);
    }

    @Override
    public <X> Object wrap(@Nullable X value, WrapperOptions options){
        return value == null ? null : fromString(value.toString());
    }
}
