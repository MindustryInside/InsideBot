package inside.data.entity.base;

import discord4j.common.util.Snowflake;

import javax.persistence.*;
import java.util.Objects;

@Embeddable
public class NamedReference{
    @Column
    private String id;

    @Column
    private String name;

    public NamedReference(){}

    public NamedReference(Snowflake id, String name){
        this.id = Objects.requireNonNull(id, "id").asString();
        this.name = Objects.requireNonNull(name, "name");
    }

    public String id(){
        return id;
    }

    public void id(String id){
        this.id = Objects.requireNonNull(id, "id");
    }

    public String name(){
        return name;
    }

    public void name(String name){
        this.name = Objects.requireNonNull(name, "name");
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        NamedReference that = (NamedReference)o;
        return id.equals(that.id) && name.equals(that.name);
    }

    @Override
    public int hashCode(){
        return Objects.hash(id, name);
    }

    @Override
    public String toString(){
        return "NamedReference{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
