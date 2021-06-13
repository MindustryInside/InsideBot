package inside.data.entity.base;

import javax.persistence.*;
import java.io.Serial;

@MappedSuperclass
public abstract class ConfigEntity extends GuildEntity{
    @Serial
    private static final long serialVersionUID = 7966224951282150244L;

    @Column
    protected boolean enabled;

    public boolean isEnabled(){
        return enabled;
    }

    public void setEnabled(boolean enabled){
        this.enabled = enabled;
    }

    @Override
    public String toString(){
        return "ConfigEntity{" +
                "enabled=" + enabled +
                "} " + super.toString();
    }
}
