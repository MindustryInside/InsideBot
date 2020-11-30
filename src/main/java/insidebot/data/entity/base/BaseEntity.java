package insidebot.data.entity.base;

import discord4j.common.util.Snowflake;
import reactor.util.annotation.NonNull;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@MappedSuperclass
public abstract class BaseEntity implements Serializable{
    private static final long serialVersionUID = 1337L;

    @Id
    protected String id;

    @NonNull
    @Transient
    public Snowflake id(){
        return Snowflake.of(id);
    }

    @NonNull
    public String getId(){
        return id;
    }

    public void id(@NonNull Snowflake id){
        this.id = id.asString();
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
        return "BaseEntity{" +
               "id='" + id + '\'' +
               '}';
    }
}
