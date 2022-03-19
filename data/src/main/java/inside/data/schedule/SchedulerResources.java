package inside.data.schedule;

import reactor.core.scheduler.Scheduler;

import java.time.Duration;
import java.util.Objects;

public class SchedulerResources {
    public static final Duration DEFAULT_BATCH_TIME_WINDOW = Duration.ZERO;
    public static final int DEFAULT_MAX_BATCH_SIZE = 3;
    public static final Duration DEFAULT_IDLE_WAIT_DURATION = Duration.ofSeconds(2);

    private final Scheduler scheduler;
    private final String instanceName;
    private final Duration batchTimeWindow;
    private final int maxBatchSize;
    private final Duration idleWaitDuration;

    public SchedulerResources(Scheduler scheduler, String instanceName) {
        this(scheduler, instanceName, DEFAULT_BATCH_TIME_WINDOW,
                DEFAULT_MAX_BATCH_SIZE, DEFAULT_IDLE_WAIT_DURATION);
    }

    public SchedulerResources(Scheduler scheduler, String instanceName, Duration batchTimeWindow,
                              int maxBatchSize, Duration idleWaitDuration) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.instanceName = Objects.requireNonNull(instanceName, "instanceName");
        this.batchTimeWindow = Objects.requireNonNull(batchTimeWindow, "batchTimeWindow");
        this.maxBatchSize = maxBatchSize;
        this.idleWaitDuration = Objects.requireNonNull(idleWaitDuration, "idleWaitDuration");
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public Duration getBatchTimeWindow() {
        return batchTimeWindow;
    }

    public int getMaxBatchSize() {
        return maxBatchSize;
    }

    public Duration getIdleWaitDuration() {
        return idleWaitDuration;
    }
}
