package insidebot.data.entity;

import insidebot.data.entity.base.UserEntity;
import reactor.util.annotation.NonNull;

import javax.persistence.*;

@Entity
@Table(name = "local_user")
public class LocalUser extends UserEntity{
    private static final long serialVersionUID = -6983332268522094510L;

    @NonNull
    @Column(length = 32)
    public String name;

    @NonNull
    @Column(length = 4)
    public String discriminator;

    @NonNull
    public String name(){
        return name;
    }

    public void name(@NonNull String name){
        this.name = name;
    }

    @NonNull
    public String discriminator(){
        return discriminator;
    }

    public void discriminator(@NonNull String discriminator){
        this.discriminator = discriminator;
    }

    @Override
    public String toString(){
        return "LocalUser{" +
               "name='" + name + '\'' +
               ", discriminator='" + discriminator + '\'' +
               ", userId='" + userId + '\'' +
               '}';
    }
}
