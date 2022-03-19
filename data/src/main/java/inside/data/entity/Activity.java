package inside.data.entity;

import inside.data.annotation.Column;
import inside.data.annotation.Entity;
import inside.data.annotation.Table;
import inside.data.entity.base.GuildEntity;
import org.immutables.value.Value;

import java.time.Instant;
import java.util.Optional;

@Entity
@Table(name = "activity")
@Value.Immutable
public interface Activity extends GuildEntity {

    static ImmutableActivity.Builder builder() {
        return ImmutableActivity.builder();
    }

    @Column(name = "user_id")
    long userId();

    @Value.Default
    @Column(name = "message_count")
    default int messageCount() {
        return 0;
    }

    @Column(name = "last_sent_message")
    Optional<Instant> lastSentMessage();

    default ImmutableActivity incrementMessageCount() {
        return ImmutableActivity.copyOf(this)
                .withMessageCount(messageCount() + 1);
    }
}
