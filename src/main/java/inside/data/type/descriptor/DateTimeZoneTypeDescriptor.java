package inside.data.type.descriptor;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.joda.time.DateTimeZone;

import java.util.*;

public class DateTimeZoneTypeDescriptor extends AbstractTypeDescriptor<DateTimeZone>{
    public static final DateTimeZoneTypeDescriptor instance = new DateTimeZoneTypeDescriptor();

    public static class DateTimeZoneComparator implements Comparator<DateTimeZone>{
        public static final DateTimeZoneComparator instance = new DateTimeZoneComparator();

        @Override
        public int compare(DateTimeZone o1, DateTimeZone o2){
            return o1.getID().compareTo(o2.getID());
        }
    }

    public DateTimeZoneTypeDescriptor(){
        super(DateTimeZone.class);
    }

    @Override
    public DateTimeZone fromString(String string){
        return DateTimeZone.forID(string);
    }

    @Override
    public Comparator<DateTimeZone> getComparator(){
        return DateTimeZoneComparator.instance;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X> X unwrap(DateTimeZone value, Class<X> type, WrapperOptions options){
        if(value == null){
            return null;
        }
        if(String.class.isAssignableFrom(type)){
            return (X)toString(value);
        }
        throw unknownUnwrap(type);
    }

    @Override
    public <X> DateTimeZone wrap(X value, WrapperOptions options){
        if(value == null){
            return null;
        }
        if(value instanceof String s){
            return fromString(s);
        }
        throw unknownWrap(value.getClass());
    }
}

