package inside.data.schedule;

import reactor.core.publisher.Mono;

import java.util.List;

public interface ReactiveScheduler {

    String ORIGINAL_FAILED_TRIGGER_KEY = "original_failed_trigger_key";

    String DEFAULT_RECOVERY_GROUP = "recovering";

    SchedulerResources resources();

    JobFactory jobFactory();

    Mono<Void> start();

    Mono<Void> scheduleJob(JobDetail jobDetail, Trigger trigger);

    Mono<Boolean> unscheduleJob(Key triggerKey);

    Mono<Boolean> unscheduleJobs(List<? extends Key> triggerKeys);

    Mono<ImmutableJobDetail> retrieveJob(Key jobKey);

    Mono<Trigger> retrieveTrigger(Key triggerKey);
}
