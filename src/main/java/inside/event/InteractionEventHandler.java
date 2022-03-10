package inside.event;

import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import inside.Configuration;
import inside.data.EntityRetriever;
import inside.data.entity.GuildConfig;
import inside.interaction.ChatInputInteractionEnvironment;
import inside.interaction.chatinput.InteractionCommandHolder;
import inside.service.InteractionService;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.Objects;

import static inside.util.ContextUtil.KEY_LOCALE;
import static inside.util.ContextUtil.KEY_TIMEZONE;

public class InteractionEventHandler extends ReactiveEventAdapter {

    private final Configuration configuration;
    private final InteractionCommandHolder interactionCommandHolder;
    private final InteractionService interactionService;
    private final EntityRetriever entityRetriever;

    public InteractionEventHandler(Configuration configuration, InteractionCommandHolder interactionCommandHolder,
                                   InteractionService interactionService, EntityRetriever entityRetriever) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.interactionCommandHolder = Objects.requireNonNull(interactionCommandHolder, "interactionCommandHolder");
        this.interactionService = Objects.requireNonNull(interactionService, "interactionService");
        this.entityRetriever = Objects.requireNonNull(entityRetriever, "entityRetriever");
    }

    @Override
    public Publisher<?> onComponentInteraction(ComponentInteractionEvent event) {
        if (event instanceof ModalSubmitInteractionEvent) { // я пока таким не пользуюсь
            return Mono.empty();
        }

        return interactionService.handleComponentInteractionEvent(event);
    }

    @Override
    public Publisher<?> onChatInputInteraction(ChatInputInteractionEvent event) {
        return Mono.justOrEmpty(event.getInteraction().getGuildId())
                .flatMap(id -> entityRetriever.getGuildConfigById(id)
                        .switchIfEmpty(entityRetriever.save(GuildConfig.builder()
                                .locale(event.getInteraction().getGuildLocale()
                                        .map(interactionService::convertLocale)
                                        .orElseThrow())
                                .guildId(id.asLong())
                                .timezone(configuration.discord().timezone())
                                .build())))
                .map(config -> Context.of(KEY_LOCALE, config.locale(),
                        KEY_TIMEZONE, config.timezone()))
                .switchIfEmpty(Mono.fromSupplier(() -> Context.of(KEY_LOCALE,
                        interactionService.convertLocale(event.getInteraction().getUserLocale()),
                        KEY_TIMEZONE, configuration.discord().timezone())))
                .map(ctx -> ChatInputInteractionEnvironment.of(configuration, interactionService, ctx, event))
                .flatMap(env -> Mono.justOrEmpty(interactionCommandHolder.getCommand(event.getCommandName()))
                        .filterWhen(command -> command.filter(env))
                        .flatMap(command -> Mono.from(command.execute(env)))
                        .contextWrite(env.context()));
    }
}
