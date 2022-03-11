package inside.data.entity;

import inside.data.annotation.Column;
import inside.data.annotation.Entity;
import inside.data.annotation.Table;
import inside.data.entity.base.GuildEntity;
import io.r2dbc.postgresql.codec.Interval;
import org.immutables.value.Value;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Entity
@Table(name = "moderation_action")
@Value.Immutable
public interface ModerationAction extends GuildEntity {

    Interval timeoutLimit = Interval.of(Duration.ofDays(28));

    static ImmutableModerationAction.Builder builder() {
        return ImmutableModerationAction.builder();
    }

    @Column(name = "admin_id")
    long adminId();

    @Column(name = "target_id")
    long targetId();

    @Column
    Type type();

    @Column
    Optional<String> reason();

    @Column(name = "end_timestamp")
    Optional<Instant> endTimestamp();

    enum Type {
        warn,
        mute
    }
}
