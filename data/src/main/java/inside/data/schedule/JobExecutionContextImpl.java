package inside.data.schedule;

import inside.util.Preconditions;
import io.netty.util.AttributeKey;
import reactor.util.annotation.Nullable;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class JobExecutionContextImpl implements JobExecutionContext {

    private final ReactiveSchedulerImpl scheduler;
    private final TriggerFiredBundle bundle;
    private final Job job;
    private final Map<String, Object> jobDataMap;

    public JobExecutionContextImpl(ReactiveSchedulerImpl scheduler, TriggerFiredBundle bundle, Job job) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.bundle = Objects.requireNonNull(bundle, "bundle");
        this.job = Objects.requireNonNull(job, "job");

        this.jobDataMap = Map.copyOf(bundle.jobDetail().jobData());
    }

    @Override
    public ReactiveScheduler scheduler() {
        return scheduler;
    }

    @Override
    public Trigger trigger() {
        return bundle.trigger();
    }

    @Override
    public boolean isRecovering() {
        return bundle.recovering();
    }

    @Override
    public Key recoveringTriggerKey() {
        Preconditions.requireState(isRecovering(), "Not a recovering job");

        return (Key) jobDataMap.get(ReactiveScheduler.ORIGINAL_FAILED_TRIGGER_KEY);
    }

    @Override
    public Map<String, Object> getMergedJobDataMap() {
        return jobDataMap;
    }

    @Override
    public JobDetail jobDetail() {
        return bundle.jobDetail();
    }

    @Override
    public Job jobInstance() {
        return job;
    }

    @Override
    public Instant fireTimestamp() {
        return bundle.fireTimestamp();
    }

    @Nullable
    @Override
    public Instant scheduledFireTimestamp() {
        return bundle.scheduledFireTimestamp();
    }

    @Nullable
    @Override
    public Instant prevFireTimeStamp() {
        return bundle.prevFireTime();
    }

    @Nullable
    @Override
    public Instant nextFireTimestamp() {
        return bundle.nextFireTime();
    }

    @Override
    public String fireInstanceId() {
        return Objects.requireNonNull(bundle.trigger().getFireInstanceId());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(AttributeKey<T> key) {
        return Optional.ofNullable((T) jobDataMap.get(key.name()));
    }

    @Override
    public String toString() {
        return "JobExecutionContextImpl{" +
                "scheduler=" + scheduler +
                ", bundle=" + bundle +
                ", job=" + job +
                '}';
    }
}
