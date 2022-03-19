package inside;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import discord4j.rest.util.Color;
import inside.service.MessageService;
import org.immutables.value.Value;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

@Value.Immutable
@JsonSerialize(as = ImmutableConfiguration.class)
@JsonDeserialize(as = ImmutableConfiguration.class)
public interface Configuration {

    default Discord discord() {
        return ImmutableDiscord.of();
    }

    default Database database() {
        return ImmutableDatabase.of();
    }

    default Tasks tasks() {
        return ImmutableTasks.of();
    }

    String token();

    @Value.Immutable(singleton = true)
    @JsonSerialize(as = ImmutableDiscord.class)
    @JsonDeserialize(as = ImmutableDiscord.class)
    interface Discord {

        @JsonProperty("embed_color")
        default Color embedColor() {
            return Color.of(0xc4f5b7);
        }

        @JsonProperty("embed_error_color")
        default Color embedErrorColor() {
            return Color.of(0xff3838);
        }

        default Duration embedErrorTtl() {
            return Duration.ofSeconds(7);
        }

        default ZoneId timezone() {
            return ZoneId.of("UTC");
        }

        default Locale locale() {
            return MessageService.supportedLocaled.get(1); // английский
        }

        default List<String> prefixes() {
            return List.of("#");
        }

        @JsonProperty("await_component_timeout")
        default Duration awaitComponentTimeout() {
            return Duration.ofMinutes(13);
        }
    }

    @Value.Immutable(singleton = true)
    @JsonSerialize(as = ImmutableDatabase.class)
    @JsonDeserialize(as = ImmutableDatabase.class)
    interface Database {

        default String url() {
            return "";
        }

        default String user() {
            return "";
        }

        default String password() {
            return "";
        }
    }

    @Value.Immutable(singleton = true)
    @JsonSerialize(as = ImmutableTasks.class)
    @JsonDeserialize(as = ImmutableTasks.class)
    interface Tasks {

        @JsonProperty("activity_check_in")
        default Duration activityCheckIn() {
            return Duration.ofMinutes(2);
        }
    }
}
