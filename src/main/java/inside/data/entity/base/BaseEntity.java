package inside.data.entity.base;

import discord4j.common.util.Snowflake;
import inside.data.type.JsonBinaryType;
import org.hibernate.annotations.*;
import reactor.util.annotation.NonNull;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@MappedSuperclass
public abstract class BaseEntity implements Serializable{
    private static final long serialVersionUID = 1337L;

    @Id
    @GenericGenerator(name = "snowflake", strategy = "inside.data.type.SnowflakeGenerator")
    @GeneratedValue(generator = "snowflake")
    protected String id;

    public String getId(){
        return id;
    }

    @Transient
    public Snowflake id(){
        return Snowflake.of(id);
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        BaseEntity that = (BaseEntity)o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode(){
        return Objects.hash(id);
    }

    @Override
    public String toString(){
        return "BaseEntity{id='" + id + "'}";
    }
}
