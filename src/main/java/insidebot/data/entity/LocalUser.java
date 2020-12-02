package insidebot.data.entity;

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

    @NonNull
    public String name(){
        return name;
    }

    public void name(@NonNull String name){
        this.name = name;
    }

    public LocalUser(){}

    public LocalUser(User user){
        userId(user.getId());
    }

    @Override
    public String toString(){
        return "LocalUser{name='" + name + "'} " + super.toString();
    }
}
