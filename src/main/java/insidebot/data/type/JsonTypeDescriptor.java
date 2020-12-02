package insidebot.data.type;

import insidebot.util.JacksonUtil;
import org.hibernate.annotations.common.reflection.java.JavaXMember;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.*;
import org.hibernate.usertype.DynamicParameterizedType;

import java.lang.reflect.*;
import java.util.*;

@SuppressWarnings({"unchecked", "deprecation", "rawtypes"})
public class JsonTypeDescriptor extends AbstractTypeDescriptor<Object> implements DynamicParameterizedType{
    private static final long serialVersionUID = -4842954367890483417L;

    private Class<?> jsonObjectClass;

    private Type type;

    public JsonTypeDescriptor(){
        super(Object.class, new MutableMutabilityPlan<>(){
            private static final long serialVersionUID = 1606718143878984537L;

            @Override
            protected Object deepCopyNotNull(Object value){
                return JacksonUtil.clone(value);
            }
        });
    }

    @Override
    public void setParameterValues(Properties parameters){
        jsonObjectClass = ((ParameterType)parameters.get(PARAMETER_TYPE)).getReturnedClass();
        try{
            Field typeField = JavaXMember.class.getDeclaredField("type");
            if(!typeField.isAccessible()){
                typeField.setAccessible(true);
            }
            type = (Type)typeField.get(parameters.get(XPROPERTY));
        }catch(IllegalAccessException | NoSuchFieldException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean areEqual(Object one, Object another){
        if(one == another) return true;
        if(one == null || another == null) return false;
        return JacksonUtil.toJsonNode(JacksonUtil.toString(one)).equals(JacksonUtil.toJsonNode(JacksonUtil.toString(another)));
    }

    @Override
    public String toString(Object value){
        return JacksonUtil.toString(value);
    }

    @Override
    public Object fromString(String string){
        if(type instanceof ParameterizedType pType){
            if(List.class.isAssignableFrom((Class)pType.getRawType())){
                return JacksonUtil.mapJsonToObjectList(string, (Class)pType.getActualTypeArguments()[0]);
            }
            if(Map.class.isAssignableFrom((Class)pType.getRawType())){
                return JacksonUtil.mapJsonToMap(string, (Class)pType.getActualTypeArguments()[0], (Class)pType.getActualTypeArguments()[1]);
            }
        }
        return JacksonUtil.fromString(string, jsonObjectClass);
    }

    @Override
    public <X> X unwrap(Object value, Class<X> type, WrapperOptions options){
        if(value == null) return null;
        if(String.class.isAssignableFrom(type)) return (X)toString(value);
        if(Object.class.isAssignableFrom(type)) return (X)JacksonUtil.toJsonNode(toString(value));
        throw unknownUnwrap(type);
    }

    @Override
    public <X> Object wrap(X value, WrapperOptions options){
        if(value == null){
            return null;
        }
        return fromString(value.toString());
    }

}
