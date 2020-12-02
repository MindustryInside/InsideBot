package insidebot.data.entity.base;

import discord4j.common.util.Snowflake;
import reactor.util.annotation.NonNull;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@MappedSuperclass
public abstract class UserEntity implements Serializable{
    private static final long serialVersionUID = -9054021239522955661L;

    @Id
    @Column(name = "user_id")
    protected String userId;

    @NonNull
    public Snowflake userId(){
        return Snowflake.of(userId);
    }

    public void userId(@NonNull Snowflake userId){
        this.userId = userId.asString();
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        UserEntity that = (UserEntity)o;
        return Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode(){
        return Objects.hash(userId);
    }

    @Override
    public String toString(){
        return "UserEntity{userId='" + userId + "'}";
    }
}
