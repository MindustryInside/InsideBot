package inside.data.type.descriptor;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import reactor.util.annotation.Nullable;

import java.sql.Timestamp;

import static org.hibernate.type.descriptor.java.JdbcTimestampTypeDescriptor.TIMESTAMP_FORMAT;

public class DateTimeTypeDescriptor extends AbstractTypeDescriptor<DateTime>{

    public static final DateTimeTypeDescriptor instance = new DateTimeTypeDescriptor();

    public DateTimeTypeDescriptor(){
        super(DateTime.class);
    }

    @Override
    public String toString(DateTime value){
        return value.toString(TIMESTAMP_FORMAT);
    }

    @Override
    public DateTime fromString(String string){
        return DateTime.parse(string, DateTimeFormat.forPattern(TIMESTAMP_FORMAT));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X> X unwrap(@Nullable DateTime value, Class<X> type, WrapperOptions options){
        if(value == null){
            return null;
        }
        if(String.class.isAssignableFrom(type)){
            return (X)toString(value);
        }
        if(Timestamp.class.isAssignableFrom(type)){
            return (X)Timestamp.valueOf(value.toString(TIMESTAMP_FORMAT));
        }
        throw unknownUnwrap(type);
    }

    @Override
    public <X> DateTime wrap(@Nullable X value, WrapperOptions options){
        if(value == null){
            return null;
        }
        if(value instanceof String s){
            return fromString(s);
        }
        if(value instanceof Timestamp t){
            return new DateTime(t);
        }
        throw unknownWrap(value.getClass());
    }
}
