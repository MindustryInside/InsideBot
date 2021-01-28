package inside.data.type;

import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.type.descriptor.*;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.BasicBinder;

import java.sql.*;

public class JsonBinarySqlTypeDescriptor extends AbstractJsonSqlTypeDescriptor{
    private static final long serialVersionUID = 925608129281277893L;

    public static final JsonBinarySqlTypeDescriptor instance = new JsonBinarySqlTypeDescriptor();

    @Override
    public <X> ValueBinder<X> getBinder(JavaTypeDescriptor<X> descriptor){
        return new BasicBinder<>(descriptor, this){
            @Override
            protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException{
                st.setObject(index, descriptor.unwrap(value, JsonNode.class, options), getSqlType());
            }

            @Override
            protected void doBind(CallableStatement st, X value, String name, WrapperOptions options) throws SQLException{
                st.setObject(name, descriptor.unwrap(value, JsonNode.class, options), getSqlType());
            }
        };
    }
}
