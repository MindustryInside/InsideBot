package insidebot.event.dispatcher;

import org.reactivestreams.Subscription;
import reactor.core.publisher.*;
import reactor.core.publisher.FluxSink.OverflowStrategy;
import reactor.core.scheduler.Scheduler;
import reactor.util.concurrent.Queues;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("deprecation")
public class DefaultEventListener implements EventListener{
    private final FluxProcessor<BaseEvent, BaseEvent> eventProcessor;
    private final FluxSink<BaseEvent> sink;
    private final Scheduler eventScheduler;

    public DefaultEventListener(FluxProcessor<BaseEvent, BaseEvent> eventProcessor,
                                OverflowStrategy overflowStrategy,
                                Scheduler eventScheduler){
        this.eventProcessor = eventProcessor;
        this.sink = eventProcessor.sink(overflowStrategy);
        this.eventScheduler = eventScheduler;
    }

    @Override
    public <E extends BaseEvent> Flux<E> on(Class<E> eventClass){
        AtomicReference<Subscription> subscription = new AtomicReference<>();
        return eventProcessor.publishOn(eventScheduler).ofType(eventClass)
                .<E>handle((event, sink) -> sink.next(event))
                .doOnSubscribe(subscription::set);
    }

    @Override
    public void publish(BaseEvent event){
        sink.next(event);
    }

    @Override
    public void shutdown(){
        sink.complete();
    }

    public static class Builder implements EventListener.Builder{
        protected FluxProcessor<BaseEvent, BaseEvent> eventProcessor;
        protected OverflowStrategy overflowStrategy = OverflowStrategy.BUFFER;
        protected Scheduler eventScheduler;

        protected Builder(){}

        @Override
        public Builder eventProcessor(FluxProcessor<BaseEvent, BaseEvent> eventProcessor){
            this.eventProcessor = Objects.requireNonNull(eventProcessor);
            return this;
        }

        @Override
        public Builder overflowStrategy(OverflowStrategy overflowStrategy){
            this.overflowStrategy = Objects.requireNonNull(overflowStrategy);
            return this;
        }

        @Override
        public Builder eventScheduler(Scheduler eventScheduler){
            this.eventScheduler = Objects.requireNonNull(eventScheduler);
            return this;
        }

        @Override
        public EventListener build(){
            if(eventProcessor == null){
                eventProcessor = EmitterProcessor.create(Queues.SMALL_BUFFER_SIZE, false);
            }
            if(eventScheduler == null){
                eventScheduler = DEFAULT_EVENT_SCHEDULER.get();
            }
            return new DefaultEventListener(eventProcessor, overflowStrategy, eventScheduler);
        }
    }
}
