package insidebot.data.model;

import lombok.*;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "message_info", schema = "public")
public class MessageInfo extends BaseEntity{
    private static final long serialVersionUID = -7977287922184407665L;

    @Id
    @Column(name = "message_id")
    private long messageId;

    @Column(name = "channel_id")
    private long channelId;

    @Column(length = 2000)
    private String content;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserInfo user;
}
