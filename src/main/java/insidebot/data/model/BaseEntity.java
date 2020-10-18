package insidebot.data.model;

import javax.persistence.MappedSuperclass;
import java.io.Serializable;

// heh
@MappedSuperclass
public abstract class BaseEntity implements Serializable{
    private static final long serialVersionUID = 1337L;
}
