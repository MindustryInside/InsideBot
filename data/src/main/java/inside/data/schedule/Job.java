package inside.data.schedule;

import org.reactivestreams.Publisher;

public interface Job {

    Publisher<?> execute(JobExecutionContext context);
}
