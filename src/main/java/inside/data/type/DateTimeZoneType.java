package inside.data.type;

import inside.data.type.descriptor.DateTimeZoneTypeDescriptor;
import org.hibernate.dialect.Dialect;
import org.hibernate.type.*;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;
import org.joda.time.DateTimeZone;

public class DateTimeZoneType extends AbstractSingleColumnStandardBasicType<DateTimeZone> implements LiteralType<DateTimeZone>{

    public static final DateTimeZoneType instance = new DateTimeZoneType();

    public DateTimeZoneType(){
        super(VarcharTypeDescriptor.INSTANCE, DateTimeZoneTypeDescriptor.instance);
    }

    @Override
    public String getName(){
        return "date-time-zone";
    }

    @Override
    protected boolean registerUnderJavaType(){
        return true;
    }

    @Override
    public String objectToSQLString(DateTimeZone value, Dialect dialect) throws Exception{
        return StringType.INSTANCE.objectToSQLString(value.getID(), dialect);
    }
}
