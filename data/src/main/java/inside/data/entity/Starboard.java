package inside.data.entity;

import inside.data.annotation.Column;
import inside.data.annotation.Entity;
import inside.data.annotation.Table;
import inside.data.entity.base.GuildEntity;
import org.immutables.value.Value;

@Entity
@Table(name = "starboard")
@Value.Immutable
public interface Starboard extends GuildEntity {

    static ImmutableStarboard.Builder builder() {
        return ImmutableStarboard.builder();
    }

    @Column(name = "source_message_id")
    long sourceMessageId();

    @Column(name = "target_message_id")
    long targetMessageId();
}
