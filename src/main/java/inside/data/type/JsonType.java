package inside.data.type;

import inside.data.type.descriptor.*;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.usertype.DynamicParameterizedType;

import java.util.Properties;

public class JsonType extends AbstractSingleColumnStandardBasicType<Object> implements DynamicParameterizedType{

    public JsonType(){
        super(JsonSqlTypeDescriptor.instance, JsonTypeDescriptor.instance);
    }

    @Override
    public String getName(){
        return "json";
    }

    @Override
    protected boolean registerUnderJavaType(){
        return true;
    }

    @Override
    public void setParameterValues(Properties parameters){
        ((JsonTypeDescriptor)getJavaTypeDescriptor()).setParameterValues(parameters);
    }
}
