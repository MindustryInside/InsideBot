package inside.data.type.descriptor;

import inside.util.*;
import org.hibernate.annotations.common.reflection.java.JavaXMember;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.*;
import org.hibernate.usertype.DynamicParameterizedType;
import reactor.util.*;
import reactor.util.annotation.Nullable;

import java.io.Serial;
import java.lang.reflect.*;
import java.util.*;

public class JsonTypeDescriptor extends AbstractTypeDescriptor<Object> implements DynamicParameterizedType{
    private static final Logger log = Loggers.getLogger(JsonTypeDescriptor.class);

    public static final JsonTypeDescriptor instance = new JsonTypeDescriptor();

    private final List<Type> types = new ArrayList<>();

    private Object xprop;

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
        xprop = parameters.get(XPROPERTY);
        Type type = get(JavaXMember.class, xprop, "type");
        types.add(type);
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
        Type ftype = get(JavaXMember.class, xprop, "type");

        return types.stream()
                .filter(type -> type.equals(ftype))
                .findFirst()
                .flatMap(type -> Try.ofCallable(() -> JacksonUtil.fromJson(string, type))
                        .onFailure(t -> log.trace("Serialization error.", t))
                        .toOptional())
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private <T> T get(Class<?> type, Object object, String name){
        try{
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return (T)field.get(object);
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
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
