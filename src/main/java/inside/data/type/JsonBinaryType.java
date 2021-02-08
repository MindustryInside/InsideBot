package inside.data.type;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.usertype.DynamicParameterizedType;

import java.io.Serial;
import java.util.Properties;

public class JsonBinaryType extends AbstractSingleColumnStandardBasicType<Object> implements DynamicParameterizedType{
    @Serial
    private static final long serialVersionUID = -3828163915941087141L;

    public JsonBinaryType(){
        super(JsonBinarySqlTypeDescriptor.instance, new JsonTypeDescriptor());
    }

    public String getName(){
        return "jsonb";
    }

    @Override
    public void setParameterValues(Properties parameters){
        ((JsonTypeDescriptor)getJavaTypeDescriptor()).setParameterValues(parameters);
    }
}
