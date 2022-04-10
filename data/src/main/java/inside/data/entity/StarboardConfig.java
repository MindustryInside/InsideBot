package inside.data.entity;

import inside.data.annotation.Column;
import inside.data.annotation.Entity;
import inside.data.annotation.Table;
import inside.data.entity.base.ConfigEntity;
import org.immutables.value.Value;

import java.util.List;

@Entity
@Table(name = "starboard_config")
@Value.Immutable
public interface StarboardConfig extends ConfigEntity {

    static ImmutableStarboardConfig.Builder builder() {
        return ImmutableStarboardConfig.builder();
    }

    @Column
    int threshold();

    @Column(name = "starboard_channel_id")
    long starboardChannelId();

    @Column
    List<EmojiDataWithPeriod> emojis();

    @Column(name = "self_starring")
    boolean selfStarring();
}
