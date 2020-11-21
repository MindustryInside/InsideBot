package insidebot.data.dao;

import arc.func.Prov;
import discord4j.common.util.Snowflake;
import insidebot.data.model.MessageInfo;
import org.hibernate.Session;
import org.hibernate.Transaction;
import reactor.core.publisher.Flux;
import reactor.util.annotation.NonNull;

import java.util.*;
import java.util.stream.Collectors;

import static insidebot.InsideBot.data;

/*
 * Статический контекст это конечно хорошо, но лучше перейти на спринг и делать сервисы
 */
public class MessageInfoDao{

    private MessageInfoDao(){}

    public static MessageInfo get(Snowflake id){
        Objects.requireNonNull(id, "Id must not be null.");
        return get(id.asLong());
    }

    public static MessageInfo get(long id){
        try(Session session = data.getSessionFactory().openSession()){
            return session.get(MessageInfo.class, id);
        }
    }

    public static MessageInfo getOr(Snowflake id, Prov<MessageInfo> prov){
        Objects.requireNonNull(id, "Id must not be null.");
        return getOr(id.asLong(), prov);
    }

    public static MessageInfo getOr(long id, Prov<MessageInfo> prov){
        try(Session session = data.getSessionFactory().openSession()){
            return session.get(MessageInfo.class, id) != null ? session.get(MessageInfo.class, id) : prov.get();
        }
    }

    @NonNull
    public static Flux<MessageInfo> all(){
        try(Session session = data.getSessionFactory().openSession()){
            return Flux.fromIterable(session.createQuery("select m from MessageInfo m", MessageInfo.class).list());
        }
    }

    @NonNull
    public static Map<Long, MessageInfo> repo(){
        try(Session session = data.getSessionFactory().openSession()){
            return session.createQuery("select m from MessageInfo m", MessageInfo.class)
                          .getResultStream()
                          .collect(Collectors.toMap(MessageInfo::getMessageId, messageInfo -> messageInfo));
        }
    }

    public static void save(MessageInfo entity){
        try(Session session = data.getSessionFactory().openSession()){
            Transaction t = session.beginTransaction();
            session.save(entity);
            t.commit();
        }
    }

    public static void saveOrUpdate(MessageInfo entity){
        try(Session session = data.getSessionFactory().openSession()){
            Transaction t = session.beginTransaction();
            session.saveOrUpdate(entity);
            t.commit();
        }
    }

    public static void update(MessageInfo entity){
        try(Session session = data.getSessionFactory().openSession()){
            Transaction t = session.beginTransaction();
            session.update(entity);
            t.commit();
        }
    }

    public static void remove(MessageInfo entity){
        try(Session session = data.getSessionFactory().openSession()){
            Transaction t = session.beginTransaction();
            session.remove(entity);
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
