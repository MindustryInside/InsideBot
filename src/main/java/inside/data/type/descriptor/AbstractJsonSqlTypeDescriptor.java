package inside.data.type.descriptor;

import org.hibernate.type.descriptor.*;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.*;

import java.io.Serial;
import java.sql.*;

public abstract class AbstractJsonSqlTypeDescriptor implements SqlTypeDescriptor{
    @Serial
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
    public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> descriptor){
        return new BasicExtractor<>(descriptor, this){
            @Override
            protected X doExtract(ResultSet resultSet, String name, WrapperOptions options) throws SQLException{
                return descriptor.wrap(resultSet.getObject(name), options);
            }

            @Override
            protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException{
                return descriptor.wrap(statement.getObject(index), options);
            }

            @Override
            protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException{
                return descriptor.wrap(statement.getObject(name), options);
            }
        };
    }
}
