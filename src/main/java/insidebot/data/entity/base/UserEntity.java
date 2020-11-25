package insidebot.data.entity.base;

import javax.persistence.*;
import java.io.Serializable;

@MappedSuperclass
public abstract class UserEntity implements Serializable{
    private static final long serialVersionUID = -9049114030237849100L;

    @Id
    @Column(name = "user_id")
    protected String userId;

    @Override
    public String toString(){
        return this.getClass().getSimpleName() + "{" +
               "userId='" + userId + '\'' +
               '}';
    }
}
