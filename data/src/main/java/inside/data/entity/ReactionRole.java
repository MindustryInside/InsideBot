package inside.data.entity;

import discord4j.discordjson.json.EmojiData;
import inside.data.annotation.Column;
import inside.data.annotation.Entity;
import inside.data.annotation.Table;
import inside.data.entity.base.GuildEntity;
import org.immutables.value.Value;

@Entity
@Table(name = "reaction_role")
@Value.Immutable
public interface ReactionRole extends GuildEntity {

    // Обусловлено лимитом платформы
    int MAX_PER_MESSAGE = 20;

    static ImmutableReactionRole.Builder builder() {
        return ImmutableReactionRole.builder();
    }

    @Column(name = "message_id")
    long messageId();

    @Column(name = "role_id")
    long roleId();

    @Column
    EmojiData emoji();
}
