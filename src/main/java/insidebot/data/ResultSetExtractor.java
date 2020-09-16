package insidebot.data;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface ResultSetExtractor<T>{

    @Nullable
    T extractData(ResultSet rs) throws SQLException;
}
