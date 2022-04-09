package inside.service.job;

import inside.data.EntityRetriever;
import inside.data.schedule.Job;
import inside.data.schedule.JobDetail;
import inside.data.schedule.JobFactory;
import inside.data.schedule.TriggerFiredBundle;
import inside.util.Try;
import io.netty.util.internal.EmptyArrays;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.lang.reflect.Constructor;
import java.util.Objects;

public class JobFactoryImpl implements JobFactory {

    private static final Logger log = Loggers.getLogger(JobFactoryImpl.class);

    private final EntityRetriever entityRetriever;

    public JobFactoryImpl(EntityRetriever entityRetriever) {
        this.entityRetriever = Objects.requireNonNull(entityRetriever, "entityRetriever");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<Job> newJob(TriggerFiredBundle bundle) {
        JobDetail jobDetail = bundle.jobDetail();
        return Mono.fromCallable(() -> {
            try {
                Class<? extends Job> jobClass = jobDetail.jobClass();
                if (log.isDebugEnabled()) {
                    log.debug("Producing instance of Job '{}' ({})", jobDetail.key(), jobClass.getName());
                }

                var constructor = Try.ofCallable(() -> (Constructor<Job>) jobClass.getDeclaredConstructor(entityRetriever.getClass()))
                        .or(Try.ofCallable(() -> (Constructor<Job>) jobClass.getDeclaredConstructor(EmptyArrays.EMPTY_CLASSES)))
                        .get();

                return constructor.newInstance();
            } catch (Exception e) {
                throw Exceptions.propagate(new IllegalStateException("Problem instantiating class '" + jobDetail.jobClass().getName() + "'", e));
            }
        });
    }
}
