package inside.data.entity;

import inside.data.entity.base.GuildEntity;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serial;
import java.time.ZoneId;
import java.util.*;

@Entity
@Table(name = "guild_config")
public class GuildConfig extends GuildEntity{
    @Serial
    private static final long serialVersionUID = 2454633035779855973L;

    @Type(type = "json")
    @Column(columnDefinition = "json")
    private List<String> prefixes;

    @Column
    private Locale locale;

    @Column(name = "time_zone")
    private ZoneId timeZone;

    public ZoneId getTimeZone(){
        return timeZone;
    }

    public static String formatPrefix(String prefix){
        Objects.requireNonNull(prefix, "prefix");
        if(prefix.chars().filter(Character::isLetter).count() >= 2 || prefix.length() > 4){
            return prefix + " ";
        }
        return prefix;
    }

    public List<String> prefixes(){
        return prefixes;
    }

    public void prefixes(List<String> prefixes){
        this.prefixes = Objects.requireNonNull(prefixes, "prefixes");
    }

    public Locale locale(){
        return locale;
    }

    public void locale(Locale locale){
        this.locale = Objects.requireNonNull(locale, "locale");
    }

    public ZoneId timeZone(){
        return timeZone;
    }

    public void timeZone(ZoneId timeZone){
        this.timeZone = Objects.requireNonNull(timeZone, "timeZone");
    }

    @Override
    public String toString(){
        return "GuildConfig{" +
                "prefixes=" + prefixes +
                ", locale=" + locale +
                ", timeZone=" + timeZone +
                "} " + super.toString();
    }
}
