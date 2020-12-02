package insidebot.data.type;

import org.hibernate.type.descriptor.*;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.*;

import java.sql.*;

public abstract class AbstractJsonSqlTypeDescriptor implements SqlTypeDescriptor{
    private static final long serialVersionUID = -8941996480499577393L;

    @Override
    public int getSqlType(){
        return Types.OTHER;
    }

    @Override
    public boolean canBeRemapped(){
        return true;
    }

    @Override
    public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor){
        return new BasicExtractor<>(javaTypeDescriptor, this){
            @Override
            protected X doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException{
                return javaTypeDescriptor.wrap(rs.getObject(name), options);
            }

            @Override
            protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException{
                return javaTypeDescriptor.wrap(statement.getObject(index), options);
            }

            @Override
            protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException{
                return javaTypeDescriptor.wrap(statement.getObject(name), options);
            }
        };
    }

}
