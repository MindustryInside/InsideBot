package inside.service.task;

import org.reactivestreams.Publisher;

import java.time.Duration;

public interface Task {

    Publisher<?> execute();

    Duration getInterval();
}
