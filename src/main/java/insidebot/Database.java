package insidebot;

import arc.util.Log;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.reflections.Reflections;
import reactor.util.annotation.NonNull;

import javax.persistence.Entity;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import static insidebot.InsideBot.settings;

public class Database{
    private final SessionFactory sessionFactory;

    public Database(){
        Log.info("Connecting to database...");

        Reflections ref = new Reflections(this.getClass().getPackageName());
        Set<Class<?>> classes = ref.getTypesAnnotatedWith(Entity.class);

        Configuration configuration = getConfig();

        classes.forEach(configuration::addAnnotatedClass);

        sessionFactory = configuration.buildSessionFactory();
    }

    private Configuration getConfig(){
        return new Configuration().setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQL9Dialect")
                                  .setProperty("hibernate.connection.driver_class", "org.postgresql.Driver")
                                  .setProperty("hibernate.connection.username", settings.get("db-username"))
                                  .setProperty("hibernate.connection.password", settings.get("db-password"))
                                  .setProperty("hibernate.connection.url", settings.get("db-url"));
    }

    public SessionFactory getSessionFactory(){
        return sessionFactory;
    }

    public String zonedFormat(){
        return DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss ZZZZ").format(ZonedDateTime.now());
    }
}
