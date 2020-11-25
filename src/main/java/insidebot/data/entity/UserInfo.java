package insidebot.data.entity;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.*;
import insidebot.data.entity.base.UserEntity;
import org.hibernate.annotations.*;
import reactor.core.publisher.Mono;
import reactor.util.annotation.*;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.*;
import java.util.*;

import static insidebot.InsideBot.listener;

@Entity
@Table(name = "user_info", schema = "public")
public class UserInfo extends UserEntity{
    private static final long serialVersionUID = -6983332268522094510L;

    @NonNull
    @Column(length = 32)
    private String name;

    @Column
    private int warns;

    @Column(name = "message_seq")
    private long messageSeq;

    @Nullable
    @Column(name = "last_sent_message")
    private Calendar lastSentMessage;

    @Nullable
    @Column(name = "mute_end_date")
    private Calendar muteEndDate;

    @NonNull
    @Fetch(FetchMode.SELECT)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<MessageInfo> messageInfo = new ArrayList<>();

    @Transient
    public int addWarn(){
        return ++warns;
    }

    @Transient
    public long addToSeq(){
        return ++messageSeq;
    }

    public void userId(@NonNull Snowflake userId){
        this.userId = userId.asString();
    }

    @NonNull
    public Snowflake userId(){
        return Snowflake.of(userId);
    }

    @NonNull
    public String name(){
        return name;
    }

    public void name(@NonNull String name){
        this.name = name;
    }

    public int warns(){
        return warns;
    }

    public void warns(int warns){
        this.warns = warns;
    }

    public long messageSeq(){
        return messageSeq;
    }

    public void messageSeq(long messageSeq){
        this.messageSeq = messageSeq;
    }

    @Nullable
    public Calendar lastSentMessage(){
        return lastSentMessage;
    }

    public void lastSentMessage(@Nullable Calendar lastSentMessage){
        this.lastSentMessage = lastSentMessage;
    }

    @Nullable
    public Calendar muteEndDate(){
        return muteEndDate;
    }

    public void muteEndDate(@Nullable Calendar muteEndDate){
        this.muteEndDate = muteEndDate;
    }

    @NonNull
    public List<MessageInfo> messageInfo(){
        return messageInfo;
    }

    public void messageInfo(@NonNull List<MessageInfo> messageInfo){
        this.messageInfo = messageInfo;
    }

    @Transient
    public Mono<User> asUser(){
        return listener.gateway.getUserById(userId());
    }

    @Transient
    public Mono<Member> asMember(){
        return listener.guild.getMemberById(userId());
    }

    @Override
    public String toString(){
        return "UserInfo{" +
               "name='" + name + '\'' +
               ", warns=" + warns +
               ", messageSeq=" + messageSeq +
               ", lastSentMessage=" + lastSentMessage +
               ", muteEndDate=" + muteEndDate +
               ", messageInfo=" + messageInfo +
               ", userId='" + userId + '\'' +
               '}';
    }
}
