package insidebot.data.dao;

import arc.func.Prov;
import discord4j.common.util.Snowflake;
import insidebot.data.model.UserInfo;
import org.hibernate.Session;
import org.hibernate.Transaction;
import reactor.core.publisher.Flux;
import reactor.util.annotation.NonNull;

import java.util.Objects;

import static insidebot.InsideBot.data;

public class UserInfoDao{

    private UserInfoDao(){}

    public static UserInfo get(Snowflake id){
        Objects.requireNonNull(id, "Id must not be null.");
        return get(id.asLong());
    }

    public static UserInfo get(long id){
        try(Session session = data.getSessionFactory().openSession()){
            return session.get(UserInfo.class, id);
        }
    }

    public static UserInfo getOr(Snowflake id, Prov<UserInfo> prov){
        Objects.requireNonNull(id, "Id must not be null.");
        return getOr(id.asLong(), prov);
    }

    public static UserInfo getOr(long id, Prov<UserInfo> prov){
        try(Session session = data.getSessionFactory().openSession()){
            return session.get(UserInfo.class, id) != null ? session.get(UserInfo.class, id) : prov.get();
        }
    }

    @NonNull
    public static Flux<UserInfo> all(){
        try(Session session = data.getSessionFactory().openSession()){
            return Flux.fromIterable(session.createQuery("select u from UserInfo u", UserInfo.class).list());
        }
    }

    public static void save(UserInfo entity){
        try(Session session = data.getSessionFactory().openSession()){
            Transaction t = session.beginTransaction();
            session.save(entity);
            t.commit();
        }
    }

    public static void update(UserInfo entity){
        try(Session session = data.getSessionFactory().openSession()){
            Transaction t = session.beginTransaction();
            session.update(entity);
            t.commit();
        }
    }

    public static void saveOrUpdate(UserInfo entity){
        try(Session session = data.getSessionFactory().openSession()){
            Transaction t = session.beginTransaction();
            session.saveOrUpdate(entity);
            t.commit();
        }
    }

    public static void remove(UserInfo entity){
        try(Session session = data.getSessionFactory().openSession()){
            Transaction t = session.beginTransaction();
            session.remove(entity);
            t.commit();
        }
    }

    public static void removeById(Snowflake id){
        Objects.requireNonNull(id, "Id must not be null.");
        removeById(id.asLong());
    }

    public static void removeById(long id){
        try(Session session = data.getSessionFactory().openSession()){
            Transaction t = session.beginTransaction();
            UserInfo info = get(id);
            if(info == null) return;
            session.remove(info);
            t.commit();
        }
    }

    public static boolean exists(Snowflake id){
        Objects.requireNonNull(id, "Id must not be null.");
        return exists(id.asLong());
    }

    public static boolean exists(long id){
        return get(id) != null;
    }
}
