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
        this.id = id;
    }

    public String name(){
        return name;
    }

    public void name(String name){
        this.name = name;
    }
}
