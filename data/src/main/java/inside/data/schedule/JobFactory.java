package inside.data.schedule;

import reactor.core.publisher.Mono;

public interface JobFactory{

    Mono<Job> newJob(TriggerFiredBundle bundle);
}
