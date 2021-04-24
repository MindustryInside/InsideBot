package inside.data.type.descriptor;

import inside.util.*;
import org.hibernate.annotations.common.reflection.java.JavaXMember;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.*;
import org.hibernate.usertype.DynamicParameterizedType;
import reactor.util.*;
import reactor.util.annotation.Nullable;

import java.io.Serial;
import java.lang.reflect.Type;
import java.util.Properties;

public class JsonTypeDescriptor extends AbstractTypeDescriptor<Object> implements DynamicParameterizedType{
    private static final Logger log = Loggers.getLogger(JsonTypeDescriptor.class);

    private Type type;

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
        JavaXMember xprop = (JavaXMember)parameters.get(XPROPERTY);
        type = xprop.getJavaType();
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
        return Try.ofCallable(() -> JacksonUtil.fromJson(string, type))
                .onFailure(t -> log.trace("Serialization error.", t))
                .orElse(null);
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
        throw unknownUnwrap(type);
    }

    @Override
    public <X> Object wrap(@Nullable X value, WrapperOptions options){
        return value == null ? null : fromString(value.toString());
    }
}
