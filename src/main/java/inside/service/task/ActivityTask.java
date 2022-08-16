package inside.service.task;

import discord4j.common.util.Snowflake;
import inside.Configuration;
import inside.Launcher;
import inside.data.EntityRetriever;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public class ActivityTask implements Task {

    private final Configuration configuration;
    private final EntityRetriever entityRetriever;

    public ActivityTask(Configuration configuration, EntityRetriever entityRetriever) {
        this.configuration = Objects.requireNonNull(configuration);
        this.entityRetriever = Objects.requireNonNull(entityRetriever);
    }

    @Override
    public Publisher<?> execute() {
        return entityRetriever.getAllEnabledActivityConfig()
                .flatMap(config -> entityRetriever.getAllActivityInGuild(Snowflake.of(config.guildId()))
                        .map(activity -> {
                            if (activity.messageCount() >= config.messageThreshold() &&
                                    activity.lastSentMessage().map(i -> i.isAfter(Instant.now().minus(config.countingInterval())))
                                            .orElse(false)) {
                                return activity;
                            }
                            return activity.withMessageCount(0);
                        })
                        .flatMap(activity -> Mono.defer(() -> {
                            if (activity.messageCount() == 0) {
                                return Launcher.getClient().rest().getGuildService()
                                        .removeGuildMemberRole(activity.guildId(), activity.userId(), config.roleId(), null);
                            }
                            return Launcher.getClient().rest().getGuildService()
                                    .addGuildMemberRole(activity.guildId(), activity.userId(), config.roleId(), null);
                        })
                        .and(entityRetriever.save(activity))));
    }

    @Override
    public Duration getInterval() {
        return configuration.tasks().activityCheckIn();
    }
}
