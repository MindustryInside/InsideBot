package insidebot.data.model;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

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

    @Column(length = 32)
    private String name;

    @Column
    private int warns;

    @Column(name = "message_seq")
    private long messageSeq;

    @Column(name = "last_sent_message")
    private Calendar lastSentMessage;

    @Column(name = "mute_end_date")
    private Calendar muteEndDate;

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
        return listener.jda.retrieveUserById(userId).complete();
    }

    @Transient
    public Member asMember(){
        return listener.guild.getMember(asUser());
    }
}
