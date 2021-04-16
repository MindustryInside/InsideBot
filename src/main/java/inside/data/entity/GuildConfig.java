package inside.data.entity;

import inside.data.entity.base.GuildEntity;
import org.joda.time.DateTimeZone;

import javax.persistence.*;
import java.io.Serial;
import java.util.*;

@Entity
@Table(name = "guild_config")
public class GuildConfig extends GuildEntity{
    @Serial
    private static final long serialVersionUID = 2454633035779855973L;

    @Column
    private String prefix;

    @Column
    private Locale locale;

    @Column(name = "time_zone")
    private DateTimeZone timeZone;

    public String prefix(){
        return prefix;
    }

    public void prefix(String prefix){
        this.prefix = Objects.requireNonNull(prefix, "prefix");
    }

    public Locale locale(){
        return locale;
    }

    public void locale(Locale locale){
        this.locale = Objects.requireNonNull(locale, "locale");
    }

    public DateTimeZone timeZone(){
        return timeZone;
    }

    public void timeZone(DateTimeZone timeZone){
        this.timeZone = Objects.requireNonNull(timeZone, "timeZone");
    }

    @Override
    public String toString(){
        return "GuildConfig{" +
                "prefix='" + prefix + '\'' +
                ", locale=" + locale +
                ", timeZone=" + timeZone +
                "} " + super.toString();
    }
}
