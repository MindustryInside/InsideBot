package insidebot.data.model;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

import javax.persistence.*;
import java.util.*;

import static insidebot.InsideBot.listener;

@Getter
@Setter
@Entity
@Table(name = "user_info", schema = "public")
public class UserInfo extends BaseEntity{
    private static final long serialVersionUID = -6983332268522094510L;

    @Id
    @Column(name = "user_id")
    private long userId;

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
        return warns += 1;
    }

    @Transient
    public void addToSeq(){
        messageSeq += 1;
    }

    @Transient
    public User asUser(){
        return listener.gateway.getUserById(Snowflake.of(userId)).block();
    }

    @Nullable
    @Transient
    public Member asMember(){
        return listener.guild.getMemberById(Snowflake.of(userId)).block();
    }
}
