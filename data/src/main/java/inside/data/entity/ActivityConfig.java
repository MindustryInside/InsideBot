package inside.data.entity;

import inside.data.annotation.Column;
import inside.data.annotation.Entity;
import inside.data.annotation.Table;
import inside.data.entity.base.ConfigEntity;
import io.r2dbc.postgresql.codec.Interval;
import org.immutables.value.Value;

@Entity
@Table(name = "activity_config")
@Value.Immutable
public interface ActivityConfig extends ConfigEntity {

    static ImmutableActivityConfig.Builder builder() {
        return ImmutableActivityConfig.builder();
    }

    @Column(name = "role_id")
    long roleId();

    @Column(name = "message_threshold")
    int messageThreshold();

    @Column(name = "counting_interval")
    Interval countingInterval();
}
