package inside.data.entity;

import inside.data.entity.base.ConfigEntity;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serial;
import java.util.*;

@Entity
@Table(name = "command_config")
public class CommandConfig extends ConfigEntity{
    @Serial
    private static final long serialVersionUID = -1216238876351831957L;

    @Column
    private String name;

    @Type(type = "json")
    @Column(columnDefinition = "jsonb") // special operator
    private List<String> aliases;

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = Objects.requireNonNull(name, "name");
    }

    public List<String> getAliases(){
        return aliases;
    }

    public void setAliases(List<String> aliases){
        this.aliases = Objects.requireNonNull(aliases, "aliases");
    }

    @Override
    public String toString(){
        return "CommandConfig{" +
                "name='" + name + '\'' +
                ", aliases=" + aliases +
                "} " + super.toString();
    }
}
