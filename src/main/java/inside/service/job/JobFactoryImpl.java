package inside.service.job;

import inside.data.EntityRetriever;
import inside.data.schedule.Job;
import inside.data.schedule.JobDetail;
import inside.data.schedule.JobFactory;
import inside.data.schedule.TriggerFiredBundle;
import io.netty.util.internal.EmptyArrays;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.lang.reflect.Constructor;
import java.util.Objects;

// TODO: лучше тут DI какой-нибудь использовать
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
        return Mono.defer(() -> {
            Class<? extends Job> jobClass = jobDetail.jobClass();
            if (log.isDebugEnabled()) {
                log.debug("Producing instance of Job '{}' ({})", jobDetail.key(), jobClass.getName());
            }

            return Mono.fromCallable(() -> (Constructor<Job>) jobClass.getDeclaredConstructor(EntityRetriever.class))
                    .onErrorResume(e -> Mono.fromCallable(() -> (Constructor<Job>) jobClass.getDeclaredConstructor(EmptyArrays.EMPTY_CLASSES)))
                    .flatMap(c -> Mono.fromCallable(() -> c.getParameterCount() > 0 ? c.newInstance(entityRetriever) : c.newInstance()));
        });
    }
}
