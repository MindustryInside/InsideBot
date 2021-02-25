package inside.data.type;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.usertype.EnhancedUserType;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.sql.*;
import java.util.Objects;

public class DateTimeType implements EnhancedUserType, Serializable{
    @Override
    public int[] sqlTypes(){
        return new int[]{Types.TIMESTAMP};
    }

    @Override
    public Class<?> returnedClass(){
        return DateTime.class;
    }

    @Override
    public boolean equals(Object a, Object b) throws HibernateException{
        return Objects.equals(a, b);
    }

    @Override
    public int hashCode(Object object) throws HibernateException{
        return Objects.hashCode(object);
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException{
        Object timestamp = StandardBasicTypes.TIMESTAMP.nullSafeGet(rs, names, session, owner);
        if(timestamp == null){
            return null;
        }
        return new DateTime(timestamp);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException{
        if(value == null){
            StandardBasicTypes.TIMESTAMP.nullSafeSet(st, null, index, session);
        }else{
            StandardBasicTypes.TIMESTAMP.nullSafeSet(st, ((DateTime)value).toDate(), index, session);
        }
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException{
        return value;
    }

    @Override
    public boolean isMutable(){
        return false;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException{
        return (Serializable)value;
    }

    @Override
    public Object assemble(Serializable cached, Object value) throws HibernateException{
        return cached;
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException{
        return original;
    }

    @Override
    public String objectToSQLString(Object object){
        throw new UnsupportedOperationException();
    }

    @Override
    public String toXMLString(Object object){
        return Objects.toString(object);
    }

    @Override
    public Object fromXMLString(String string){
        return new DateTime(string);
    }
}

