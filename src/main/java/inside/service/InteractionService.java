package inside.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import inside.Configuration;
import inside.data.EntityRetriever;
import inside.data.entity.GuildConfig;
import inside.interaction.ButtonInteractionEnvironment;
import inside.interaction.ComponentInteractionEnvironment;
import inside.interaction.SelectMenuInteractionEnvironment;
import inside.interaction.component.ButtonListener;
import inside.interaction.component.ComponentListener;
import inside.interaction.component.SelectMenuListener;
import inside.util.ContextUtil;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.function.TupleUtils;
import reactor.util.context.Context;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public class InteractionService extends BaseService {
    public static final String CUSTOM_ID_PREFIX = "inside-";

    private final Configuration configuration;
    private final MessageService messageService;
    private final EntityRetriever entityRetriever;

    private final ConcurrentMap<String, ComponentListener> componentListeners = new ConcurrentHashMap<>();
    private final Cache<String, Tuple2<Snowflake, ComponentListener>> componentInteractions;

    public InteractionService(GatewayDiscordClient client, Configuration configuration, MessageService messageService,
                              EntityRetriever entityRetriever) {
        super(client);
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.messageService = Objects.requireNonNull(messageService, "messageService");
        this.entityRetriever = Objects.requireNonNull(entityRetriever, "entityRetriever");

        this.componentInteractions = Caffeine.newBuilder()
                .expireAfterWrite(configuration.discord().awaitComponentTimeout())
                .build();
    }

    private Mono<ComponentListener> findComponentListener(ComponentInteractionEnvironment env) {
        String customId = env.event().getCustomId();
        if (!customId.startsWith(CUSTOM_ID_PREFIX)) {
            return Mono.empty();
        }

        return Mono.justOrEmpty(componentListeners.get(customId))
                .switchIfEmpty(Mono.justOrEmpty(componentInteractions.getIfPresent(customId))
                        .switchIfEmpty(messageService.err(env, "Это взаимодействие недоступно.\n" +
                                "Вызовите команду повторно, чтобы начать новое.").then(Mono.never()))
                        .filter(TupleUtils.predicate((userId, listener) -> userId.equals(
                                env.event().getInteraction().getUser().getId())))
                        .map(Tuple2::getT2)
                        .switchIfEmpty(messageService.err(env, "Вы не можете участвовать в чужом взаимодействии.\n" +
                                "Вызовите команду повторно, чтобы начать новое.").then(Mono.never())))
                .switchIfEmpty(messageService.err(env, "Это взаимодействие недоступно.\n" +
                        "Вызовите команду повторно, чтобы начать новое.").then(Mono.never()));
    }

    public Publisher<?> handleComponentInteractionEvent(ComponentInteractionEvent event) {
        return Mono.justOrEmpty(event.getInteraction().getGuildId())
                .flatMap(id -> entityRetriever.getGuildConfigById(id)
                        .switchIfEmpty(entityRetriever.save(GuildConfig.builder()
                                .locale(event.getInteraction().getGuildLocale()
                                        .map(this::convertLocale)
                                        .orElseThrow())
                                .guildId(id.asLong())
                                .timezone(configuration.discord().timezone())
                                .build())))
                .map(config -> Context.of(ContextUtil.KEY_LOCALE, config.locale(),
                        ContextUtil.KEY_TIMEZONE, config.timezone()))
                .switchIfEmpty(Mono.fromSupplier(() -> Context.of(ContextUtil.KEY_LOCALE,
                        convertLocale(event.getInteraction().getUserLocale()),
                        ContextUtil.KEY_TIMEZONE, configuration.discord().timezone())))
                .flatMap(ctx -> {
                    if (event instanceof ButtonInteractionEvent b) {
                        var env = ButtonInteractionEnvironment.of(configuration, this, ctx, b);

                        return findComponentListener(env)
                                .cast(ButtonListener.class)
                                .flatMap(listener -> Mono.from(listener.handle(env)))
                                .contextWrite(ctx);
                    } else if (event instanceof SelectMenuInteractionEvent s) {
                        var env = SelectMenuInteractionEnvironment.of(configuration, this, ctx, s);

                        return findComponentListener(env)
                                .cast(SelectMenuListener.class)
                                .flatMap(listener -> Mono.from(listener.handle(env)))
                                .contextWrite(ctx);
                    } else {
                        return Mono.error(new IllegalStateException());
                    }
                });

    }

    public Locale convertLocale(String language) {
        int sep = language.indexOf('-');
        if (sep != -1) {
            language = language.substring(0, sep);
        }
        Locale locale = new Locale(language);
        return MessageService.supportedLocaled.contains(locale) ? locale : configuration.discord().locale();
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
