package inside.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import inside.Configuration;
import inside.interaction.ButtonInteractionEnvironment;
import inside.interaction.SelectMenuInteractionEnvironment;
import inside.interaction.component.ButtonListener;
import inside.interaction.component.ComponentListener;
import inside.interaction.component.SelectMenuListener;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.function.TupleUtils;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public class InteractionService extends BaseService {
    public static final String CUSTOM_ID_PREFIX = "inside-";

    private final Configuration configuration;
    private final MessageService messageService;

    private final ConcurrentMap<String, ComponentListener> componentListeners = new ConcurrentHashMap<>();
    private final Cache<String, Tuple2<Snowflake, ComponentListener>> componentInteractions;

    public InteractionService(GatewayDiscordClient client, Configuration configuration, MessageService messageService) {
        super(client);
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.messageService = Objects.requireNonNull(messageService, "messageService");
        this.componentInteractions = Caffeine.newBuilder()
                .expireAfterWrite(configuration.discord().awaitComponentTimeout())
                .build();
    }

    private Mono<ComponentListener> findComponentListener(ComponentInteractionEvent event) {
        String customId = event.getCustomId();
        if (!customId.startsWith(CUSTOM_ID_PREFIX)) {
            return Mono.empty();
        }

        return Mono.justOrEmpty(componentListeners.get(customId))
                .switchIfEmpty(Mono.justOrEmpty(componentInteractions.getIfPresent(customId))
                        .switchIfEmpty(messageService.err(event, "Это взаимодействие недоступно.\n" +
                                "Вызовите команду повторно, чтобы начать новое.").then(Mono.never()))
                        .filter(TupleUtils.predicate((userId, listener) -> userId.equals(event.getInteraction().getUser().getId())))
                        .map(Tuple2::getT2)
                        .switchIfEmpty(messageService.err(event, "Вы не можете участвовать в чужом взаимодействии.\n" +
                                "Вызовите команду повторно, чтобы начать новое.").then(Mono.never())))
                .switchIfEmpty(messageService.err(event, "Это взаимодействие недоступно.\n" +
                        "Вызовите команду повторно, чтобы начать новое.").then(Mono.never()));
    }

    public Publisher<?> handleButtonInteractionEvent(ButtonInteractionEvent event) {
        return findComponentListener(event)
                .cast(ButtonListener.class)
                .flatMap(listener -> Mono.from(listener.handle(
                        ButtonInteractionEnvironment.of(configuration, this, event))));
    }

    public Publisher<?> handleSelectMenuInteractionEvent(SelectMenuInteractionEvent event) {
        return findComponentListener(event)
                .cast(SelectMenuListener.class)
                .flatMap(listener -> Mono.from(listener.handle(
                        SelectMenuInteractionEnvironment.of(configuration, this, event))));
    }

    public void registerComponentListener(ComponentListener listener) {
        componentListeners.put(listener.getCustomId(), listener);
    }

    private void registerComponentInteraction(Snowflake userId, String customId, ComponentListener listener) {
        componentInteractions.put(customId, Tuples.of(userId, listener));
    }

    public <R> Mono<R> awaitButtonInteraction(Snowflake userId, String customId,
                                              Function<? super ButtonInteractionEnvironment, ? extends Publisher<R>> listener) {
        return Mono.defer(() -> {
            Sinks.One<R> sink = Sinks.one();

            var delegate = new ButtonListenerDelegate<>(listener, sink);
            registerComponentInteraction(userId, customId, delegate);

            return sink.asMono();
        });
    }

    public <R> Mono<R> awaitSelectMenuInteraction(Snowflake userId, String customId,
                                                  Function<? super SelectMenuInteractionEnvironment, ? extends Publisher<R>> listener) {
        return Mono.defer(() -> {
            Sinks.One<R> sink = Sinks.one();

            var delegate = new SelectMenuListenerDelegate<>(listener, sink);
            registerComponentInteraction(userId, customId, delegate);

            return sink.asMono();
        });
    }

    private static class ButtonListenerDelegate<R> implements ButtonListener {

        private final Function<? super ButtonInteractionEnvironment, ? extends Publisher<R>> delegate;
        private final Sinks.One<? super R> sink;

        public ButtonListenerDelegate(Function<? super ButtonInteractionEnvironment, ? extends Publisher<R>> delegate,
                                      Sinks.One<? super R> sink) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
            this.sink = Objects.requireNonNull(sink, "sink");
        }

        @Override
        public Publisher<?> handle(ButtonInteractionEnvironment env) {
            return Mono.from(delegate.apply(env))
                    .doOnSuccess(value -> {
                        if (value == null) {
                            sink.emitEmpty(Sinks.EmitFailureHandler.FAIL_FAST);
                        } else {
                            sink.emitValue(value, Sinks.EmitFailureHandler.FAIL_FAST);
                        }
                    })
                    .doOnError(t -> sink.emitError(t, Sinks.EmitFailureHandler.FAIL_FAST));
        }
    }

    private static class SelectMenuListenerDelegate<R> implements SelectMenuListener {

        private final Function<? super SelectMenuInteractionEnvironment, ? extends Publisher<R>> delegate;
        private final Sinks.One<? super R> sink;

        public SelectMenuListenerDelegate(Function<? super SelectMenuInteractionEnvironment, ? extends Publisher<R>> delegate,
                                          Sinks.One<? super R> sink) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
            this.sink = Objects.requireNonNull(sink, "sink");
        }

        @Override
        public Publisher<?> handle(SelectMenuInteractionEnvironment env) {
            return Mono.from(delegate.apply(env))
                    .doOnSuccess(value -> {
                        if (value == null) {
                            sink.emitEmpty(Sinks.EmitFailureHandler.FAIL_FAST);
                        } else {
                            sink.emitValue(value, Sinks.EmitFailureHandler.FAIL_FAST);
                        }
                    })
                    .doOnError(t -> sink.emitError(t, Sinks.EmitFailureHandler.FAIL_FAST));
        }
    }
}
