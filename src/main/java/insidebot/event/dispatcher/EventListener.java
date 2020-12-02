package insidebot.event.dispatcher;

import org.reactivestreams.Publisher;
import org.slf4j.*;
import reactor.core.publisher.*;
import reactor.core.scheduler.Scheduler;
import reactor.scheduler.forkjoin.ForkJoinPoolScheduler;

import java.time.Duration;
import java.util.function.*;

public interface EventListener{
    Logger log = LoggerFactory.getLogger(EventListener.class);
    Supplier<Scheduler> DEFAULT_EVENT_SCHEDULER = () -> ForkJoinPoolScheduler.create("inside-events");

    static Builder builder(){
        return new DefaultEventListener.Builder();
    }

    static EventListener buffering(){
        return builder().build();
    }

    static EventListener withEarliestEvents(int bufferSize){
        return builder().eventProcessor(EmitterProcessor.create(bufferSize, false))
                        .overflowStrategy(FluxSink.OverflowStrategy.DROP)
                        .build();
    }

    static EventListener withLatestEvents(int bufferSize){
        return builder().eventProcessor(EmitterProcessor.create(bufferSize, false))
                        .overflowStrategy(FluxSink.OverflowStrategy.LATEST)
                        .build();
    }

    static EventListener replayingWithTimeout(Duration maxAge){
        return new DefaultEventListener(
                ReplayProcessor.createTimeout(maxAge),
                FluxSink.OverflowStrategy.IGNORE,
                DEFAULT_EVENT_SCHEDULER.get()
        );
    }

    static EventListener replayingWithSize(int historySize){
        return new DefaultEventListener(
                ReplayProcessor.create(historySize),
                FluxSink.OverflowStrategy.IGNORE,
                DEFAULT_EVENT_SCHEDULER.get()
        );
    }

    <E extends BaseEvent> Flux<E> on(Class<E> eventClass);

    default <E extends BaseEvent, T> Flux<T> on(Class<E> eventClass, Function<E, Publisher<T>> mapper){
        return on(eventClass).flatMap(event -> Flux.defer(() -> mapper.apply(event)).onErrorResume(t -> Mono.empty()));
    }

    default Flux<BaseEvent> on(Events adapter){
        return on(BaseEvent.class).flatMap(event -> Flux.defer(() -> adapter.hookOnEvent(event))
                                  .onErrorResume(t -> Mono.empty())
                                  .then(Mono.just(event)));
    }

    void publish(BaseEvent event);

    void shutdown();

    interface Builder{
        DefaultEventListener.Builder eventProcessor(FluxProcessor<BaseEvent, BaseEvent> eventProcessor);

        DefaultEventListener.Builder overflowStrategy(FluxSink.OverflowStrategy overflowStrategy);

        DefaultEventListener.Builder eventScheduler(Scheduler eventScheduler);

        EventListener build();
    }
}
