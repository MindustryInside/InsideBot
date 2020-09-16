package insidebot.data;

import arc.struct.Array;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RowExtractor<T> implements ResultSetExtractor<Array<T>>{

    private final RowMapper<T> rowMapper;
    private final int rowsExpected;

    public RowExtractor(RowMapper<T> rowMapper){
        this(rowMapper, 0);
    }

    public RowExtractor(RowMapper<T> rowMapper, int rowsExpected){
        if(rowMapper == null) throw new NullPointerException("RowMapper is required");
        this.rowMapper = rowMapper;
        this.rowsExpected = rowsExpected;
    }

    @Nullable
    @Override
    public Array<T> extractData(ResultSet rs) throws SQLException{
        if(rs == null) throw new NullPointerException("ResultSet is required");
        Array<T> results = (this.rowsExpected > 0 ? new Array<>(this.rowsExpected) : new Array<>());
        int rowNum = 0;
        while(rs.next()){
            results.add(this.rowMapper.mapRow(rs, rowNum++));
        }
        return results;
    }
}
