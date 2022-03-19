package inside.data.schedule;

import io.netty.util.internal.EmptyArrays;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.lang.reflect.Constructor;

public class DefaultJobFactory implements JobFactory {
    private static final Logger log = Loggers.getLogger(DefaultJobFactory.class);

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

                Constructor<Job> constructor = (Constructor<Job>) jobClass.getDeclaredConstructor(EmptyArrays.EMPTY_CLASSES);
                return constructor.newInstance();
            } catch (Exception e) {
                throw Exceptions.propagate(new IllegalStateException("Problem instantiating class '" + jobDetail.jobClass().getName() + "'", e));
            }
        });
    }
}
