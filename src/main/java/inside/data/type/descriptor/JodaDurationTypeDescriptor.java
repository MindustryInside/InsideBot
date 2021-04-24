package inside.data.type.descriptor;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.joda.time.Duration;
import reactor.util.annotation.Nullable;

public class JodaDurationTypeDescriptor extends AbstractTypeDescriptor<Duration>{

    public static final JodaDurationTypeDescriptor instance = new JodaDurationTypeDescriptor();

    public JodaDurationTypeDescriptor(){
        super(Duration.class);
    }

    @Override
    public Duration fromString(String string){
        if(string == null){
            return null;
        }
        return Duration.parse(string);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X> X unwrap(@Nullable Duration duration, Class<X> type, WrapperOptions options){
        if(duration == null){
            return null;
        }
        if(Duration.class.isAssignableFrom(type)){
            return (X)duration;
        }
        if(String.class.isAssignableFrom(type)){
            return (X)duration.toString();
        }
        throw unknownUnwrap(type);
    }

    @Override
    public <X> Duration wrap(@Nullable X value, WrapperOptions options){
        if(value == null){
            return null;
        }
        if(value instanceof Duration d){
            return d;
        }
        if(value instanceof String s){
            return Duration.parse(s);
        }
        throw unknownWrap(value.getClass());
    }
}
