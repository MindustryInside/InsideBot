package inside.data.entity;

import discord4j.discordjson.Id;
import inside.data.annotation.Column;
import inside.data.annotation.Entity;
import inside.data.annotation.Table;
import inside.data.entity.base.ConfigEntity;
import io.r2dbc.postgresql.codec.Interval;
import org.immutables.value.Value;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Entity
@Table(name = "moderation_config")
@Value.Immutable
public interface ModerationConfig extends ConfigEntity {

    static ImmutableModerationConfig.Builder builder() {
        return ImmutableModerationConfig.builder();
    }

    @Column(name = "warn_expire_interval")
    Optional<Interval> warnExpireInterval();

    @Column(name = "mute_base_interval")
    Optional<Interval> muteBaseInterval();

    @Column(name = "threshold_punishments")
    Optional<Map<Long, PunishmentSettings>> thresholdPunishments();

    @Column(name = "admin_role_ids")
    Optional<Set<Id>> adminRoleIds();

    @Column(name = "mute_role_id")
    Optional<Long> muteRoleId();

    @Column(name = "ping_spam_threshold")
    Optional<Integer> pingSpamThreshold();
}
