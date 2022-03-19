package inside.data.schedule;

import io.netty.util.AttributeKey;
import reactor.util.annotation.Nullable;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public interface JobExecutionContext{

    ReactiveScheduler scheduler();

    Trigger trigger();

    boolean isRecovering();

    Key recoveringTriggerKey();

    JobDetail jobDetail();

    Job jobInstance();

    Instant fireTimestamp();

    @Nullable
    Instant scheduledFireTimestamp();

    @Nullable
    Instant prevFireTimeStamp();

    @Nullable
    Instant nextFireTimestamp();

    String fireInstanceId();

    Map<String, Object> getMergedJobDataMap();

    <T> Optional<T> get(AttributeKey<T> key);
}
