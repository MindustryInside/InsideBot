package inside.data.type;

import inside.data.type.descriptor.DateTimeTypeDescriptor;
import org.hibernate.dialect.Dialect;
import org.hibernate.type.*;
import org.hibernate.type.descriptor.sql.TimestampTypeDescriptor;
import org.joda.time.DateTime;

public class DateTimeType extends AbstractSingleColumnStandardBasicType<DateTime> implements LiteralType<DateTime>{

    public static final DateTimeType instance = new DateTimeType();

    public DateTimeType(){
        super(TimestampTypeDescriptor.INSTANCE, DateTimeTypeDescriptor.instance);
    }

    @Override
    public String getName(){
        return "date-time";
    }

    @Override
    protected boolean registerUnderJavaType(){
        return true;
    }

    @Override
    public String objectToSQLString(DateTime value, Dialect dialect) throws Exception{
        return TimestampType.INSTANCE.objectToSQLString(value.toDate(), dialect);
    }
}

