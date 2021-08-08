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

    public Snowflake getMessageId(){
        return Snowflake.of(messageId);
    }

    public void setMessageId(Snowflake messageId){
        this.messageId = Objects.requireNonNull(messageId, "messageId").asLong();
    }

    public Snowflake getRoleId(){
        return Snowflake.of(roleId);
    }

    public void setRoleId(Snowflake roleId){
        this.roleId = Objects.requireNonNull(roleId, "roleId").asLong();
    }

    public EmojiData getEmoji(){
        return emoji;
    }

    public void setEmoji(EmojiData emoji){
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
