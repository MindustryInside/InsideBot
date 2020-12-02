package insidebot.data.entity;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.User;
import insidebot.data.entity.base.UserEntity;
import reactor.util.annotation.NonNull;

import javax.persistence.*;

@Entity
@Table(name = "local_user")
public class LocalUser extends UserEntity{
    private static final long serialVersionUID = -6983332268522094510L;

    @Column(length = 32)
    public String name;

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

    public LocalUser(){}

    public LocalUser(User user){
        userId(user.getId());
        discriminator(user.getDiscriminator());
    }

    @Override
    public String toString(){
        return "LocalUser{" +
               "name='" + name + '\'' +
               ", discriminator='" + discriminator + '\'' +
               "} " + super.toString();
    }
}
