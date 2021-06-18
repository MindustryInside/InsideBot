package inside.data.entity;

import discord4j.common.util.Snowflake;
import discord4j.discordjson.json.EmojiData;
import inside.data.entity.base.GuildEntity;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serial;
import java.util.Objects;

@Entity
@Table(name = "emoji_dispenser")
public class EmojiDispenser extends GuildEntity{
    @Serial
    private static final long serialVersionUID = 5877593911682778304L;

    @Column(name = "message_id")
    private long messageId;

    @Column(name = "role_id")
    private long roleId;

    @Type(type = "json")
    @Column(columnDefinition = "json")
    private EmojiData emoji;

    public Snowflake messageId(){
        return Snowflake.of(messageId);
    }

    public void messageId(Snowflake messageId){
        this.messageId = Objects.requireNonNull(messageId, "messageId").asLong();
    }

    public Snowflake roleId(){
        return Snowflake.of(roleId);
    }

    public void roleId(Snowflake roleId){
        this.roleId = Objects.requireNonNull(roleId, "roleId").asLong();
    }

    public EmojiData emoji(){
        return emoji;
    }

    public void emoji(EmojiData emoji){
        this.emoji = Objects.requireNonNull(emoji, "emoji");
    }

    @Override
    public String toString(){
        return "EmojiDispenser{" +
                "messageId=" + messageId +
                ", roleId=" + roleId +
                ", emoji=" + emoji +
                "} " + super.toString();
    }
}

