package insidebot.data.entity.base;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@MappedSuperclass
public abstract class BaseEntity implements Serializable{
    private static final long serialVersionUID = 1337L;

    @Id
    protected String id;

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
        return this.getClass().getSimpleName() + "{" +
               "id='" + id + '\'' +
               '}';
    }
}
