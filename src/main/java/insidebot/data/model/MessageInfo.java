package insidebot.data.model;

import lombok.*;
import reactor.util.annotation.NonNull;

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

    @NonNull
    @Column(length = 2000)
    private String content;

    @NonNull
    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserInfo user;
}
