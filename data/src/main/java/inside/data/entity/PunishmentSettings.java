package inside.data.entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import discord4j.discordjson.possible.Possible;
import io.r2dbc.postgresql.codec.Interval;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutablePunishmentSettings.class)
@JsonDeserialize(as = ImmutablePunishmentSettings.class)
public interface PunishmentSettings {

    static ImmutablePunishmentSettings.Builder builder() {
        return ImmutablePunishmentSettings.builder();
    }

    ModerationAction.Type type();

    Possible<Optional<Interval>> interval();
}
