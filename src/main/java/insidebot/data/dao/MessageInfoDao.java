package insidebot.data.dao;

import arc.func.Prov;
import insidebot.data.model.MessageInfo;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static insidebot.InsideBot.data;

public class MessageInfoDao{

    private MessageInfoDao(){}

    public static MessageInfo get(long id){
        try(Session session = data.getSessionFactory().openSession()){
            return session.get(MessageInfo.class, id);
        }
    }

    public static MessageInfo getOr(long id, Prov<MessageInfo> prov){
        try(Session session = data.getSessionFactory().openSession()){
            return session.get(MessageInfo.class, id) != null ? session.get(MessageInfo.class, id) : prov.get();
        }
    }

    public static List<MessageInfo> getAll(){
        try(Session session = data.getSessionFactory().openSession()){
            return session.createQuery("SELECT a FROM MessageInfo a", MessageInfo.class).getResultList();
        }
    }

    public static Map<Long, MessageInfo> repo(){
        try(Session session = data.getSessionFactory().openSession()){
            return session.createQuery("SELECT a FROM MessageInfo a", MessageInfo.class)
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

    public static boolean exists(long id){
        try(Session session = data.getSessionFactory().openSession()){
            return session.get(MessageInfo.class, id) != null;
        }
    }
}
